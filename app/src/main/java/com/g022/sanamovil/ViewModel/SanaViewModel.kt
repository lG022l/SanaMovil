package com.g022.sanamovil.ViewModel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.g022.sanamovil.EmergencyLevel
import com.g022.sanamovil.UiState
import com.g022.sanamovil.database.AppDatabase
import com.g022.sanamovil.database.ClinicalDecisionLog
import com.g022.sanamovil.engine.ClinicalRuleEngine
import com.g022.sanamovil.engine.ExplanationGenerator
import com.g022.sanamovil.engine.ResponseLibrary
import com.g022.sanamovil.engine.RiskLevel
import com.g022.sanamovil.engine.SymptomExtractor
import com.g022.sanamovil.engine.TriageResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.google.gson.reflect.TypeToken
import com.g022.sanamovil.UserRole
import com.g022.sanamovil.OperadorStats
import com.g022.sanamovil.AlertaSana
import com.g022.sanamovil.AlertaNivel

// IMPORTANTE: Cambiamos "ViewModel()" por "AndroidViewModel(application)"
// para poder acceder a la base de datos sin problemas de Contexto.
class SanaViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(UiState())
        private set

    // isLlamaLoaded y isWhisperLoaded indican el estado de los modelos locales en C++
    var isLlamaLoaded = false
    var isWhisperLoaded = false

    // Variable clásica para enlazar la función nativa de C++ (1 solo bloque)
    var generateLlamaResponse: ((String) -> String)? = null

    // Variable para enlazar la nueva función nativa de streaming
    var generateLlamaStream: ((String, (String) -> Unit) -> Unit)? = null

    // Historial ficticio para el menú lateral
    var recentQueries = mutableStateListOf<String>()

    // INSTANCIAS DEL NUEVO MOTOR DE DECISIÓN CLÍNICA
    private val symptomExtractor = SymptomExtractor()
    private val ruleEngine = ClinicalRuleEngine()
    private val explanationGenerator = ExplanationGenerator()

    // Variable para guardar el texto original del usuario
    private var originalUserInput: String = ""

    fun updateInput(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun setLoading(isLoading: Boolean, message: String = "") {
        uiState = uiState.copy(isLoading = isLoading, statusMessage = message)
    }

    fun setResult(resultObj: TriageResult, level: EmergencyLevel) {
        uiState = uiState.copy(
            triageResult = resultObj,
            analysisResult = resultObj.llmExplanation,
            emergencyLevel = level,
            isLoading = false,
            statusMessage = ""
        )

        val preview = "${resultObj.actionType.label} - ${resultObj.urgencyLevel.title}"
        recentQueries.add(0, preview)
    }

    private fun mapRiskToEmergencyLevel(riskLevel: RiskLevel): EmergencyLevel {
        return when (riskLevel) {
            RiskLevel.RED -> EmergencyLevel.EMERGENCIA
            RiskLevel.ORANGE -> EmergencyLevel.SEVERO
            RiskLevel.YELLOW -> EmergencyLevel.MODERADO
            RiskLevel.GREEN -> EmergencyLevel.LEVE
            RiskLevel.BLUE -> EmergencyLevel.NONE
        }
    }

    // --- FUNCIONES DEL WIZARD (FASE 8) ---
    private var temporarySymptoms: com.g022.sanamovil.engine.StructuredSymptoms? = null

    fun updateWizardAge(age: String) { uiState = uiState.copy(wizardAge = age) }
    fun updateWizardDuration(duration: String) { uiState = uiState.copy(wizardDuration = duration) }
    fun updateWizardIntensity(intensity: Float) {
        uiState = uiState.copy(wizardIntensity = intensity)
        if (intensity > 7f) {
            uiState = uiState.copy(askAboutConsciousness = true)
        } else {
            uiState = uiState.copy(askAboutConsciousness = false, hasLossOfConsciousness = false)
        }
    }
    fun updateConsciousness(loss: Boolean) { uiState = uiState.copy(hasLossOfConsciousness = loss) }
    fun updateRadiation(radiates: Boolean) { uiState = uiState.copy(hasRadiatingPain = radiates) }

    fun updateWizardConsent(accepted: Boolean) {
        uiState = uiState.copy(wizardConsentAccepted = accepted)
    }

    fun processTriage(transcription: String) {
        if (!isLlamaLoaded) {
            setLoading(false, "Error: El modelo de IA no está cargado aún.")
            return
        }

        originalUserInput = transcription
        val lowerText = transcription.lowercase()
        val asksRadiation = lowerText.contains("pecho") || lowerText.contains("corazón")

        temporarySymptoms = com.g022.sanamovil.engine.StructuredSymptoms(
            intensity = 5,
            age = 0,
            isConscious = true,
            radiatingPain = false
        )

        uiState = uiState.copy(
            isLoading = false,
            statusMessage = "",
            showWizard = true,
            askAboutRadiation = asksRadiation,
            wizardIntensity = 5f
        )
    }

    fun submitWizardAndCalculate() {
        val temp = temporarySymptoms ?: return

        val consentimientoAceptado = uiState.wizardConsentAccepted

        val finalSymptoms = temp.copy(
            age = uiState.wizardAge.toIntOrNull() ?: temp.age,
            intensity = uiState.wizardIntensity.toInt(),
            isConscious = !uiState.hasLossOfConsciousness,
            radiatingPain = uiState.hasRadiatingPain || temp.radiatingPain
        )

        uiState = uiState.copy(showWizard = false)

        viewModelScope.launch {
            setLoading(true, "Calculando nivel de riesgo...")
            withContext(Dispatchers.IO) {

                val tiempoInicioMs = System.currentTimeMillis()


                val engineEval = ruleEngine.evaluateSymptoms(finalSymptoms)
                val clinicalRiskLevel = engineEval.riskLevel
                val triggeredRulesList = engineEval.triggeredRules

                // Corregido: Llamar a buildExplanationPrompt con los parámetros correctos
                val explanationPrompt = explanationGenerator.buildExplanationPrompt(
                    originalUserText = originalUserInput, // <-- Solo agrega esta línea
                    symptoms = finalSymptoms,
                    riskLevel = clinicalRiskLevel
                )

                val urgency = ResponseLibrary.mapRiskToUrgency(clinicalRiskLevel)
                val emergencyLevelUi = mapRiskToEmergencyLevel(clinicalRiskLevel)

                var currentTriageResult = TriageResult(
                    urgencyLevel = urgency,
                    timeframe = ResponseLibrary.getTimeframe(urgency),
                    actionType = ResponseLibrary.getActionType(urgency),
                    standardMessage = ResponseLibrary.getStandardMessage(urgency),
                    llmExplanation = "",
                    triggeredRules = triggeredRulesList
                )

                withContext(Dispatchers.Main) {
                    setResult(currentTriageResult, emergencyLevelUi)
                }

                var accumulatedExplanation = ""

                generateLlamaStream?.invoke(explanationPrompt) { token ->
                    accumulatedExplanation += token
                    val safeExplanation = explanationGenerator.validateAndFilterResponse(accumulatedExplanation)

                    viewModelScope.launch(Dispatchers.Main) {
                        currentTriageResult = currentTriageResult.copy(llmExplanation = safeExplanation)
                        uiState = uiState.copy(
                            triageResult = currentTriageResult,
                            analysisResult = safeExplanation
                        )
                    }
                }

                val tiempoFinMs = System.currentTimeMillis()
                val duracionTotalProcesamiento = tiempoFinMs - tiempoInicioMs



                // 4. GUARDADO FINAL EN BASE DE DATOS (ACTUALIZADO)
                guardarLogAuditoria(
                    context = getApplication(),
                    inputUsuario = originalUserInput,
                    respuestaIA = explanationGenerator.validateAndFilterResponse(accumulatedExplanation),
                    reglas = engineEval.triggeredRules.joinToString(", "), // Corrección menor sugerida aquí
                    nivelRiesgo = engineEval.riskLevel.name,
                    consentimiento = consentimientoAceptado,
                    tiempoProcesamiento = duracionTotalProcesamiento
                )
            }
        }
    }

    // --- NAVEGACIÓN Y ROLES ---
    fun setRole(role: UserRole, context: Context? = null) {
        uiState = uiState.copy(currentRole = role)
        // Si entramos al modo supervisor, cargamos los datos
        if (role == UserRole.SUPERVISOR && context != null) {
            cargarMetricasDashboard(context)
            cargarListaDeCasos(context) // Solo la llamamos, no la creamos aquí adentro
            cargarConfiguracionLocal(context)
        }
    }

    // 👇 DEFINIMOS LAS FUNCIONES AFUERA PARA QUE LA UI LAS PUEDA VER 👇

    fun setFiltroPrioridad(filtro: String) {
        uiState = uiState.copy(filtroPrioridadActivo = filtro)
    }

    fun procesarMetricasYAlertas(logs: List<ClinicalDecisionLog>) {
        if (logs.isEmpty()) return

        // 1. Calcular métricas por operador (idDispositivoOrigen)
        val stats = logs.groupBy { it.idDispositivoOrigen }.mapValues { (id, lista) ->
            OperadorStats(
                nombre = id,
                totalPacientes = lista.size,
                tiempoPromedioMs = lista.map { it.tiempoProcesamientoMs }.average().toLong(),
                porcIncompletos = 0f, // Por ahora 0, luego podemos medir si falta el output
                distribucionRiesgo = lista.groupBy { it.nivelRiesgoAsignado }.mapValues { it.value.size }
            )
        }

        // 2. Generar Alertas Inteligentes
        val nuevasAlertas = mutableListOf<AlertaSana>()

        // Alerta: Tiempo promedio > 3 min (180,000 ms)
        val tiempoGralMs = logs.map { it.tiempoProcesamientoMs }.average()
        if (tiempoGralMs > 180000) {
            nuevasAlertas.add(AlertaSana("Atención Lenta", "El tiempo promedio supera los 3 min.", AlertaNivel.CRITICAL))
        }

        // Alerta: Muchos casos críticos (Rojos)
        val rojos = logs.count { it.nivelRiesgoAsignado.contains("RED") }
        if (rojos > logs.size * 0.3) {
            nuevasAlertas.add(AlertaSana("Saturación Crítica", "Más del 30% de casos son de alta prioridad.", AlertaNivel.WARNING))
        }

        uiState = uiState.copy(
            metricasPorOperador = stats,
            alertasSistema = nuevasAlertas
        )
    }
    fun cargarListaDeCasos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).clinicalDecisionDao()
                val logs = dao.getAllAuditLogs()

                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(dashboardLogs = logs)
                }
            } catch (e: Exception) {
                println("Error al cargar lista de casos: ${e.message}")
            }
        }
    }
    // --- FASE 3.5: SELECCIÓN DE CASOS ---
    fun setCasoSeleccionado(caso: ClinicalDecisionLog?) {
        uiState = uiState.copy(casoSeleccionadoParaDetalle = caso)
    }

    fun setSupervisorTab(tabIndex: Int) {
        uiState = uiState.copy(supervisorSelectedTab = tabIndex)
    }
    fun cargarMetricasDashboard(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).clinicalDecisionDao()
                val logs = dao.getAllAuditLogs()

                if (logs.isNotEmpty()) {
                    // 1. Total de Casos
                    val total = logs.size

                    // 2. Tiempo Promedio (convertimos los milisegundos a formato "Xm Ys")
                    val tiempoTotalMs = logs.sumOf { it.tiempoProcesamientoMs }
                    val promedioMs = tiempoTotalMs / total
                    val minutos = (promedioMs / 1000) / 60
                    val segundos = (promedioMs / 1000) % 60
                    val textoTiempo = if (minutos > 0) "${minutos}m ${segundos}s" else "${segundos}s"

                    // 3. Distribución (Contamos cuántos hay de cada nivel)
                    // Nota: Asegúrate de que los nombres coincidan con los que guardas (ej. EMERGENCIA, SEVERO, etc.)
                    val rojos = logs.count { it.nivelRiesgoAsignado.contains("RED", ignoreCase = true) || it.nivelRiesgoAsignado.contains("EMERGENCIA", ignoreCase = true) || it.nivelRiesgoAsignado.contains("SEVERO", ignoreCase = true) }
                    val amarillos = logs.count { it.nivelRiesgoAsignado.contains("YELLOW", ignoreCase = true) || it.nivelRiesgoAsignado.contains("MODERADO", ignoreCase = true) }
                    val verdes = logs.count { it.nivelRiesgoAsignado.contains("GREEN", ignoreCase = true) || it.nivelRiesgoAsignado.contains("LEVE", ignoreCase = true) || it.nivelRiesgoAsignado.contains("NONE", ignoreCase = true) }

                    // 4. Operadores Activos (Contamos cuántos dispositivos únicos hay)
                    val operadores = logs.map { it.idDispositivoOrigen }.distinct().size

                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            dashTotalCasos = total,
                            dashTiempoPromedio = textoTiempo,
                            dashRojos = rojos,
                            dashAmarillos = amarillos,
                            dashVerdes = verdes,
                            dashOperadoresActivos = operadores
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error al cargar métricas: ${e.message}")
            }
        }
    }

    fun setLegacyResult(text: String, level: EmergencyLevel) {
        uiState = uiState.copy(
            analysisResult = text,
            emergencyLevel = level,
            isLoading = false,
            statusMessage = ""
        )
        recentQueries.add(0, "Simulación - ${level.label}")
    }

    /**
     * Guarda el historial inalterable para auditorías clínicas (COFEPRIS/Fase 10)
     */
    private fun guardarLogAuditoria(
        context: Context,
        inputUsuario: String,
        respuestaIA: String,
        reglas: String,
        nivelRiesgo: String,
        consentimiento: Boolean,         // NUEVO
        tiempoProcesamiento: Long        // NUEVO
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).clinicalDecisionDao()

                // Instanciamos el Log con los campos recién agregados
                val log = ClinicalDecisionLog(
                    inputCompleto = inputUsuario,
                    outputGenerado = respuestaIA,
                    reglasActivadas = reglas,
                    nivelRiesgoAsignado = nivelRiesgo,
                    versionAlgoritmo = "v1.0.0",
                    consentimientoAceptado = consentimiento,
                    tiempoProcesamientoMs = tiempoProcesamiento,
                    // ID de dispositivo por ahora lo dejamos estático,
                    // después lo puedes jalar de SharedPreferences
                    idDispositivoOrigen = "DISPOSITIVO_BRIGADA_01"
                )

                dao.insertDecisionLog(log)
                println("✅ AUDITORÍA: Caso guardado. Riesgo: $nivelRiesgo, Tiempo: ${tiempoProcesamiento}ms, Consentimiento: $consentimiento")

                cargarListaDeCasos(context)
                cargarMetricasDashboard(context)
            } catch (e: Exception) {
                println("❌ ERROR AUDITORÍA: No se pudo guardar el log - ${e.message}")
            }
        }
    }

    // En SanaViewModel.kt
    fun exportarDatos(context: Context, onResult: (Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).clinicalDecisionDao()
                val logs = dao.getAllAuditLogs() //

                // 1. Convertir a JSON
                val jsonString = Gson().toJson(logs)

                // 2. Encriptar
                val encryptedData = CryptoUtils.encrypt(jsonString)

                // 3. Crear archivo en la carpeta de caché para compartir
                val file = File(context.cacheDir, "SanaMovil_Backup_${System.currentTimeMillis()}.sana")
                file.writeText(encryptedData)

                // 4. Obtener URI segura para compartir
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                withContext(Dispatchers.Main) { onResult(uri) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    // En SanaViewModel.kt
    fun importarDatos(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Leer el contenido del URI
                val encryptedData = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

                if (encryptedData != null) {
                    // 2. Desencriptar
                    val decryptedJson = CryptoUtils.decrypt(encryptedData)

                    // 3. Convertir de JSON a Lista
                    val listType = object : TypeToken<List<ClinicalDecisionLog>>() {}.type
                    val logs: List<ClinicalDecisionLog> = Gson().fromJson(decryptedJson, listType)

                    // 4. Guardar en la base de datos local del supervisor
                    val dao = AppDatabase.getDatabase(context).clinicalDecisionDao()
                    dao.insertAll(logs)

                    withContext(Dispatchers.Main) {
                        // Notificar éxito en la UI
                        uiState = uiState.copy(statusMessage = "✅ Importación exitosa: ${logs.size} registros añadidos")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(statusMessage = "❌ Error al importar: Archivo inválido o corrupto")
                }
            }
        }
    }

    // --- FASE 6: FUNCIONES DE CONFIGURACIÓN ---
    fun updateConfigNombre(nombre: String) { uiState = uiState.copy(configNombreBrigada = nombre) }
    fun updateConfigNivel(nivel: String) { uiState = uiState.copy(configNivelRecursos = nivel) }
    fun updateConfigContacto(contacto: String) { uiState = uiState.copy(configContactoEmergencia = contacto) }

    fun guardarConfiguracionLocal(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Creamos un mapa con los datos
                val configMap = mapOf(
                    "nombre_brigada" to uiState.configNombreBrigada,
                    "nivel_recursos" to uiState.configNivelRecursos,
                    "contacto_emergencia" to uiState.configContactoEmergencia,
                    "ultima_actualizacion" to System.currentTimeMillis().toString()
                )

                // Lo convertimos a JSON
                val jsonString = Gson().toJson(configMap)

                // Lo guardamos en la memoria interna (invisible para el usuario normal)
                val file = File(context.filesDir, "deployment_config.json")
                file.writeText(jsonString)

                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(statusMessage = "✅ Configuración guardada correctamente")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(statusMessage = "❌ Error al guardar JSON")
                }
            }
        }
    }

    fun cargarConfiguracionLocal(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "deployment_config.json")
                if (file.exists()) {
                    val jsonString = file.readText()

                    // Decodificamos el JSON
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val configMap: Map<String, String> = Gson().fromJson(jsonString, type)

                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            configNombreBrigada = configMap["nombre_brigada"] ?: "",
                            configNivelRecursos = configMap["nivel_recursos"] ?: "Básico",
                            configContactoEmergencia = configMap["contacto_emergencia"] ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                println("No se pudo cargar la config (o no existe aún)")
            }
        }
    }



}