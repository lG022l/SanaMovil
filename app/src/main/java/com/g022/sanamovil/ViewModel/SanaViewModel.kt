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

class SanaViewModel : ViewModel() {
    var uiState by mutableStateOf(UiState())
        private set

    // isLlamaLoaded y isWhisperLoaded indican el estado de los modelos locales en C++
    var isLlamaLoaded = false
    var isWhisperLoaded = false

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

    fun setResult(result: String, level: EmergencyLevel) {
        uiState = uiState.copy(
            analysisResult = result,
            emergencyLevel = level,
            isLoading = false
        )
        // Agregar al historial si hay un resultado válido
        if (result.isNotEmpty()) {
            val preview = result.take(30).replace("\n", " ") + "..."
            recentQueries.add(0, preview)
        }
    }

    /**
     * FASE 7: EL NUEVO FLUJO ORQUESTADOR
     * Llama a esta función cuando Whisper termine de transcribir el audio
     * o cuando el usuario presione el botón de "Analizar texto".
     */
    fun processTriage(transcription: String) {
        if (!isLlamaLoaded) {
            setLoading(false, "Error: El modelo de IA no está cargado aún.")
            return
        }

        viewModelScope.launch {
            try {
                // Paso 1: Notificar a la UI
                setLoading(true, "Extrayendo síntomas de la transcripción...")

                // Hacemos el trabajo pesado en un hilo secundario para no congelar la UI
                withContext(Dispatchers.IO) {
                    // FASE 1: EXTRAER DATOS
                    val extractionPrompt = symptomExtractor.buildExtractionPrompt(transcription)

                    // TODO: Reemplaza esto con tu llamada real a Llama en C++ (llama-lib.cpp)
                    val rawJsonFromLlama = generateWithLocalLlm(extractionPrompt)

                    val structuredSymptoms = symptomExtractor.parseLlmResponseToSymptoms(rawJsonFromLlama)

                    // FASE 2: MOTOR DE REGLAS DETERMINISTA (Sin IA)
                    setLoading(true, "Calculando nivel de riesgo (Triaje)...")
                    val clinicalRiskLevel = ruleEngine.evaluateSymptoms(structuredSymptoms)

                    // FASE 3: GENERAR EXPLICACIÓN (Con IA)
                    setLoading(true, "Generando recomendación segura...")
                    val explanationPrompt = explanationGenerator.buildExplanationPrompt(structuredSymptoms, clinicalRiskLevel)

                    // TODO: Reemplaza esto con tu llamada real a Llama en C++
                    val rawExplanationFromLlama = generateWithLocalLlm(explanationPrompt)

                    // FASE 4: GUARDRAILS DE SEGURIDAD
                    val safeExplanation = explanationGenerator.validateAndFilterResponse(rawExplanationFromLlama)

                    // FASE 5: MAPEO Y ACTUALIZACIÓN DE UI
                    // Convertimos el RiskLevel clínico al EmergencyLevel visual que ya tienes en tu repo
                    val emergencyLevelUi = mapRiskToEmergencyLevel(clinicalRiskLevel)

                    // Volvemos al hilo principal para actualizar la UI de Compose
                    withContext(Dispatchers.Main) {
                        setResult(safeExplanation, emergencyLevelUi)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false, "Ocurrió un error en el motor: ${e.message}")
                }
            }
        }
    }

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
        // Aquí mandarías llamar a tu función nativa.
        // Ej: return nativeLlamaGenerate(prompt)

        // Simulación para que compile y pruebes:
        return "Simulación de respuesta del LLM local..."
    }
}