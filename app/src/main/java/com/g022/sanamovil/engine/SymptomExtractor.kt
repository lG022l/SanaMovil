package com.g022.sanamovil.engine

import com.google.gson.Gson

class SymptomExtractor {

    private val gson = Gson()

    // Este es el System Prompt que obligará a tu LLM a comportarse como un simple extractor de datos
    fun buildExtractionPrompt(transcript: String): String {
        return """
            Eres un analizador de datos médicos. Tu única tarea es leer la transcripción del paciente y extraer la información en formato JSON estrictamente basado en el siguiente esquema.
            Si un dato no se menciona, usa null para números y false para booleanos.
            NO agregues texto adicional, saludos ni explicaciones, SOLO devuelve el objeto JSON.
            
            Esquema JSON esperado:
            {
              "age": int o null,
              "durationHours": int o null,
              "intensity": int (1-10) o null,
              "relevantHistory": [string],
              "isConscious": boolean,
              "associatedSymptoms": [string],
              "hasChestPain": boolean,
              "radiatingPain": boolean,
              "hasSevereBleeding": boolean,
              "hasBreathingDifficulty": boolean,
              "hasHighFever": boolean
            }
            
            Transcripción del paciente: "$transcript"
        """.trimIndent()
    }

    /**
     * Esta función toma el JSON devuelto por tu LLM y lo convierte en tu Data Class.
     */
    fun parseLlmResponseToSymptoms(llmJsonResponse: String): StructuredSymptoms {
        return try {
            // Limpiamos la respuesta por si el LLM incluye formato markdown (```json ... ```)
            val cleanJson = llmJsonResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()

            gson.fromJson(cleanJson, StructuredSymptoms::class.java)
        } catch (e: Exception) {
            // Fallback de seguridad: si el LLM falla o alucina, no crasheamos la app.
            StructuredSymptoms(
                isConscious = true,
                associatedSymptoms = listOf("Error de extracción: se requiere evaluación manual")
            )
        }
    }
}