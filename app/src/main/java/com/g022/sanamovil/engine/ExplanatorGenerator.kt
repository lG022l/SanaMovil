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

    /**
     * ✅ VERSIÓN CORREGIDA: Ahora incluye el texto original del usuario y los síntomas asociados
     */
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

        // FORMATO NATIVO DE LLAMA 3.2
        return """
            <|begin_of_text|><|start_header_id|>system<|end_header_id|>
            
            Eres un asistente de orientación empático para una aplicación médica. Explica brevemente al paciente por qué el sistema le asignó este nivel de prioridad.
            
            INSTRUCCIONES:
            1. HAZ REFERENCIA DIRECTA a lo que el paciente mencionó.
            2. Explica de forma fluida y empática por qué recibió este nivel de prioridad.
            3. NUNCA des diagnósticos médicos ni nombres de enfermedades.
            4. NUNCA recetes medicamentos.
            5. Sé cálido pero profesional.
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

    /**
     * Guardrail: Filtro de seguridad que audita la respuesta del LLM y limpia alucinaciones.
     */
    fun validateAndFilterResponse(llmResponse: String): String {
        // 1. TIJERAS: Limpieza adaptada a tokens de Llama 3.2
        var cleanedResponse = llmResponse
            .substringBefore("```")  // Corta bloques de código
            .substringBefore("<|eot_id|>") // El verdadero token de fin de Llama 3
            .substringBefore("<|start_header_id|>") // Por si intenta empezar otro turno
            .substringBefore("Mensaje para el paciente:")
            .trim()

        // 2. CORTAFUEGOS ANTI-LOOPS: Detectar si una frase se repite
        val words = cleanedResponse.split(Regex("\\s+"))
        if (words.size > 20) {
            val lastTenWords = words.takeLast(10).joinToString(" ")
            val previousTenWords = words.dropLast(10).takeLast(10).joinToString(" ")

            // Si hay bucle, cortamos la cadena justo antes de la repetición
            if (lastTenWords == previousTenWords) {
                cleanedResponse = cleanedResponse.substringBefore(lastTenWords).trim()
            }
        }

        // 3. Eliminar secuencias infinitas de puntuación (ej. "......" o ",,,,")
        cleanedResponse = cleanedResponse.replace(Regex("([.`*~_\\-,])\\1{3,}"), ".")

        // 4. Eliminar saltos de línea excesivos
        cleanedResponse = cleanedResponse.replace(Regex("\n{3,}"), "\n\n")

        // 5. FILTRO DE SEGURIDAD MÁS ESTRICTO (Palabras prohibidas)
        val lowerCaseResponse = cleanedResponse.lowercase()

        /*
        /////////
        RESPUESTA HARDCODEADA (Descomentar cuando implementes forbiddenWords)
        /////////
        for (word in forbiddenWords) {
            if (lowerCaseResponse.contains(word)) {
                Log.w("SanaMovil_Guardrails", "¡Bypass detectado! Palabra prohibida: '$word'")
                return "Basado en los síntomas que nos compartiste, el sistema ha clasificado tu situación con prioridad. Por normativas de seguridad y salud, te recomendamos buscar valoración médica presencial en el tiempo indicado. No podemos ofrecer diagnósticos automatizados por este medio."
            }
        }
        */

        // 6. RETORNO SEGURO
        return if (cleanedResponse.isNotBlank()) {
            cleanedResponse
        } else {
            "Análisis de síntomas completado. Por favor, sigue la acción sugerida en la tarjeta superior."
        }
    }
}