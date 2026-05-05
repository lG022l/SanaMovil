package com.g022.sanamovil.engine

import android.util.Log

class ExplanationGenerator {

    // Palabras prohibidas que dispararán el bloqueo de seguridad
    private val forbiddenWords = listOf(
        // Diagnósticos y afirmaciones
        "diagnóstico", "diagnostico", "tienes", "enfermedad", "padeces",
        "es probable que tengas", "tu problema es", "sufres de", "lo que tienes es",
        "infección", "inflamación", "virus", "bacteria", "síndrome", "trastorno",
        "infarto", "apendicitis", "fractura", "esguince", "covid", "cáncer", "tumor",

        // Medicamentos y tratamientos
        "receto", "tratamiento", "pastillas", "cura", "medicamento", "dosis",
        "receta", "prescripción", "prescribo", "paracetamol", "ibuprofeno",
        "aspirina", "antibiótico", "analgésico", "jarabe", "pomada", "inyección",
        "remedio", "terapia", "toma", "miligramos", "mg", "gotas",

        // Falsas garantías
        "no es nada", "no es grave", "no te preocupes", "es normal", "es común",
        "te aseguro", "seguramente", "se te pasará", "va a desaparecer",

        // Acciones médicas
        "cirugía", "operación", "operar", "internar", "hospitalizar"
    )

    fun buildExplanationPrompt(
        originalUserText: String,
        symptoms: StructuredSymptoms,
        riskLevel: RiskLevel
    ): String {
        val specificSymptoms = if (symptoms.associatedSymptoms.isNotEmpty()) {
            symptoms.associatedSymptoms.joinToString(", ")
        } else {
            "Síntomas generales"
        }

        // PROMPT MEJORADO: Enfocado en síntesis y conclusión, prohibiendo el "parroting"
        return """
            <|begin_of_text|><|start_header_id|>system<|end_header_id|>
            
            Eres un asistente de orientación empático para una aplicación médica. Explica brevemente al paciente por qué el sistema le asignó este nivel de prioridad.
            
            INSTRUCCIONES:
            1. Ve directo a la conclusión. NO repitas ni resumas los síntomas que el paciente ya te dio.
            2. Explica con empatía por qué la gravedad de la situación requiere este nivel de prioridad.
            3. Tu rol es estrictamente de apoyo y orientación. Limítate a sugerir la evaluación médica correspondiente.
            4. Mantén la respuesta en 2 o 3 oraciones como máximo. Sé cálido y profesional.
            <|eot_id|><|start_header_id|>user<|end_header_id|>
            
            CONTEXTO DEL PACIENTE:
            El paciente describió: "$originalUserText"
            
            DATOS CLÍNICOS EXTRAÍDOS:
            - Síntomas mencionados: $specificSymptoms
            - Nivel de Riesgo asignado: ${riskLevel.name} (${riskLevel.description})
            - Intensidad del dolor: ${symptoms.intensity ?: "No especificado"}/10
            - Fiebre alta: ${if(symptoms.hasHighFever) "Sí" else "No"}
            - Dificultad para respirar: ${if(symptoms.hasBreathingDifficulty) "Sí" else "No"}
            ${if(symptoms.age != null) "- Edad: ${symptoms.age} años" else ""}
            <|eot_id|><|start_header_id|>assistant<|end_header_id|>
            
            
        """.trimIndent()
    }

    fun validateAndFilterResponse(llmResponse: String): String {
        // 1. TIJERAS: Limpieza adaptada a tokens de Llama 3.2
        var cleanedResponse = llmResponse
            .substringBefore("```")
            .substringBefore("<|eot_id|>")
            .substringBefore("<|start_header_id|>")
            .substringBefore("Mensaje para el paciente:")
            .trim()

        // 2. CORTAFUEGOS ANTI-LOOPS: Funciona como red de seguridad secundaria al repetition_penalty
        val words = cleanedResponse.split(Regex("\\s+"))
        if (words.size > 20) {
            val lastTenWords = words.takeLast(10).joinToString(" ")
            val previousTenWords = words.dropLast(10).takeLast(10).joinToString(" ")

            if (lastTenWords == previousTenWords) {
                cleanedResponse = cleanedResponse.substringBeforeLast(lastTenWords).trim()
            }
        }

        // 3. Limpieza de artefactos de generación
        cleanedResponse = cleanedResponse.replace(Regex("([.`*~_\\-,])\\1{3,}"), ".")
        cleanedResponse = cleanedResponse.replace(Regex("\n{3,}"), "\n\n")

        // 4. FILTRO DE SEGURIDAD (Activado): Audita la respuesta final del LLM
        val lowerCaseResponse = cleanedResponse.lowercase()


        /*

        RESPUESTA HARDCODEADA, NO EN USO AUN
        for (word in forbiddenWords) {
            if (lowerCaseResponse.contains(word)) {
                Log.w("SanaMovil_Guardrails", "¡Bypass detectado! Palabra prohibida: '${word}'")
                return "Basado en los síntomas que nos compartiste, el sistema ha clasificado tu situación con la prioridad indicada. Por normativas de seguridad y salud, te recomendamos buscar valoración médica presencial en el tiempo sugerido. No podemos ofrecer diagnósticos automatizados por este medio."
            }
        }

        */


        // 5. RETORNO SEGURO
        return if (cleanedResponse.isNotBlank()) {
            cleanedResponse
        } else {
            "Análisis de síntomas completado. Por favor, sigue la acción sugerida en la tarjeta superior."
        }
    }
}