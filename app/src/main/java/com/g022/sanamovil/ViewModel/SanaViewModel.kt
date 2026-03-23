package com.g022.sanamovil.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g022.sanamovil.EmergencyLevel
import com.g022.sanamovil.UiState
import com.g022.sanamovil.engine.ClinicalRuleEngine
import com.g022.sanamovil.engine.ExplanationGenerator
import com.g022.sanamovil.engine.RiskLevel
import com.g022.sanamovil.engine.SymptomExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.g022.sanamovil.engine.TriageResult
import com.g022.sanamovil.engine.ResponseLibrary

class SanaViewModel : ViewModel() {
    var uiState by mutableStateOf(UiState())
        private set

    // isLlamaLoaded y isWhisperLoaded indican el estado de los modelos locales en C++
    var isLlamaLoaded = false
    var isWhisperLoaded = false

    // Variable para enlazar la función nativa de C++
    var generateLlamaResponse: ((String) -> String)? = null

    // Historial ficticio para el menú lateral
    var recentQueries = mutableStateListOf<String>()

    // --- INSTANCIAS DEL NUEVO MOTOR DE DECISIÓN CLÍNICA ---
    private val symptomExtractor = SymptomExtractor()
    private val ruleEngine = ClinicalRuleEngine()
    private val explanationGenerator = ExplanationGenerator()

    fun updateInput(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun setLoading(isLoading: Boolean, message: String = "") {
        uiState = uiState.copy(isLoading = isLoading, statusMessage = message)
    }

    fun setResult(resultObj: TriageResult, level: EmergencyLevel) {
        uiState = uiState.copy(
            triageResult = resultObj, // Guardamos el objeto completo para la auditoría y la UI
            analysisResult = resultObj.llmExplanation, // Mantenemos la explicación para compatibilidad
            emergencyLevel = level,
            isLoading = false,
            statusMessage = ""
        )

        // Agregar al historial usando la acción recomendada
        val preview = "${resultObj.actionType.label} - ${resultObj.urgencyLevel.title}"
        recentQueries.add(0, preview)
    }

    /**
     * FASE 7: EL NUEVO FLUJO ORQUESTADOR
     * Llama a esta función cuando Whisper termine de transcribir el audio
     * o cuando el usuario presione el botón de "Analizar texto".
     */

    /**
     * Mapea el resultado del motor clínico determinista a los colores/estados
     * que ya tienes definidos en UiState.kt
     */
    private fun mapRiskToEmergencyLevel(riskLevel: RiskLevel): EmergencyLevel {
        return when (riskLevel) {
            RiskLevel.RED -> EmergencyLevel.EMERGENCIA
            RiskLevel.ORANGE -> EmergencyLevel.SEVERO
            RiskLevel.YELLOW -> EmergencyLevel.MODERADO
            RiskLevel.GREEN -> EmergencyLevel.LEVE
            RiskLevel.BLUE -> EmergencyLevel.NONE
        }
    }

    /**
     * Función Mockup: Aquí es donde debes conectar tu código JNI que llama a llama-lib.cpp
     */
    private fun generateWithLocalLlm(prompt: String): String {
        // Llamamos a la función nativa real si está conectada
        return generateLlamaResponse?.invoke(prompt) ?: "Error: Conexión con el modelo LLM falló."
    }


    // --- FUNCIONES DEL WIZARD (FASE 8) ---

    // 1. Guardamos temporalmente los síntomas extraídos por el LLM
    private var temporarySymptoms: com.g022.sanamovil.engine.StructuredSymptoms? = null

    // 2. Actualizadores de estado para los inputs del usuario
    fun updateWizardAge(age: String) { uiState = uiState.copy(wizardAge = age) }
    fun updateWizardDuration(duration: String) { uiState = uiState.copy(wizardDuration = duration) }
    fun updateWizardIntensity(intensity: Float) {
        uiState = uiState.copy(wizardIntensity = intensity)
        // Lógica Dinámica: Si la intensidad es mayor a 7, preguntamos por consciencia
        if (intensity > 7f) {
            uiState = uiState.copy(askAboutConsciousness = true)
        } else {
            uiState = uiState.copy(askAboutConsciousness = false, hasLossOfConsciousness = false)
        }
    }
    fun updateConsciousness(loss: Boolean) { uiState = uiState.copy(hasLossOfConsciousness = loss) }
    fun updateRadiation(radiates: Boolean) { uiState = uiState.copy(hasRadiatingPain = radiates) }

    // 3. Modificamos processTriage para que sea INSTANTÁNEO
    fun processTriage(transcription: String) {
        if (!isLlamaLoaded) {
            setLoading(false, "Error: El modelo de IA no está cargado aún.")
            return
        }

        // 1. Detección rápida de palabras clave (sin usar la IA, toma 0.01 segundos)
        val lowerText = transcription.lowercase()
        val asksRadiation = lowerText.contains("pecho") || lowerText.contains("corazón")

        // 2. Creamos los datos temporales directamente con el texto del usuario
        temporarySymptoms = com.g022.sanamovil.engine.StructuredSymptoms(
            intensity = 5,
            age = 0,
            isConscious = true,
            radiatingPain = false
        )

        // 3. Mostramos el Wizard inmediatamente, apagando el estado de carga
        uiState = uiState.copy(
            isLoading = false,
            statusMessage = "",
            showWizard = true,
            askAboutRadiation = asksRadiation,
            wizardIntensity = 5f
        )
    }

    // 4. Esta función se llama cuando el usuario le da "Continuar" en el Wizard
    fun submitWizardAndCalculate() {
        val temp = temporarySymptoms ?: return

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

                // AQUI ESTÁ LA MAGIA DE LA FASE 9
                // 1. Obtenemos la evaluación que ahora incluye las reglas activadas (Auditoría)
                val engineEval = ruleEngine.evaluateSymptoms(finalSymptoms)
                val clinicalRiskLevel = engineEval.riskLevel
                val triggeredRules = engineEval.triggeredRules

                // 2. Generamos la explicación empática con el LLM
                val explanationPrompt = explanationGenerator.buildExplanationPrompt(finalSymptoms, clinicalRiskLevel)
                val rawExplanationFromLlama = generateWithLocalLlm(explanationPrompt)
                val safeExplanation = explanationGenerator.validateAndFilterResponse(rawExplanationFromLlama)

                // 3. Obtenemos los textos legales inmutables de nuestra biblioteca
                val urgency = ResponseLibrary.mapRiskToUrgency(clinicalRiskLevel)

                // 4. Armamos el paquete final blindado legalmente
                val finalTriageResult = TriageResult(
                    urgencyLevel = urgency,
                    timeframe = ResponseLibrary.getTimeframe(urgency),
                    actionType = ResponseLibrary.getActionType(urgency),
                    standardMessage = ResponseLibrary.getStandardMessage(urgency),
                    llmExplanation = safeExplanation,
                    triggeredRules = triggeredRules
                )

                val emergencyLevelUi = mapRiskToEmergencyLevel(clinicalRiskLevel)

                withContext(Dispatchers.Main) {
                    // Enviamos todo el paquete a la UI
                    setResult(finalTriageResult, emergencyLevelUi)
                }
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
        // Agregar al historial
        recentQueries.add(0, "Simulación - ${level.label}")
    }



}