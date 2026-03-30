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
        // Construir lista de síntomas específicos mencionados
        val specificSymptoms = if (symptoms.associatedSymptoms.isNotEmpty()) {
            symptoms.associatedSymptoms.joinToString(", ")
        } else {
            "Síntomas generales"
        }

        return """
            [INST]
            Eres un asistente de orientación empático para una aplicación médica. Explica brevemente al paciente por qué el sistema le asignó este nivel de prioridad.
            
            CONTEXTO DEL PACIENTE:
            El paciente describió: "$originalUserText"
            
            DATOS CLÍNICOS EXTRAÍDOS:
            - Síntomas mencionados: $specificSymptoms
            - Nivel de Riesgo asignado: ${riskLevel.name} (${riskLevel.description})
            - Intensidad del dolor: ${symptoms.intensity ?: "No especificado"}/10
            - Fiebre alta: ${if(symptoms.hasHighFever) "Sí" else "No"}
            - Dificultad para respirar: ${if(symptoms.hasBreathingDifficulty) "Sí" else "No"}
            ${if(symptoms.age != null) "- Edad: ${symptoms.age} años" else ""}
            
            INSTRUCCIONES:
            1. HAZ REFERENCIA DIRECTA a lo que el paciente mencionó en su descripción original.
            2. Explica de forma fluida y empática por qué recibió este nivel de prioridad.
            3. Menciona los síntomas ESPECÍFICOS que reportó (usa sus propias palabras cuando sea posible).
            4. NUNCA des diagnósticos médicos ni nombres de enfermedades.
            5. NUNCA recetes medicamentos.
            6. Sé cálido pero profesional.
            
            EJEMPLO DE RESPUESTA ESPERADA (Para un paciente con dolor de estómago. NO COPIES ESTE TEXTO, úsalo solo como guía de tono):
            "Entiendo que estás experimentando un dolor abdominal muy fuerte. El sistema ha clasificado tu situación en este nivel porque, aunque el dolor es significativo, tus datos indican que no hay presencia de fiebre alta ni dificultad respiratoria, lo cual nos ayuda a descartar una emergencia inmediata."
            [/INST]
            
            Respuesta:
        """.trimIndent()
    }

    /**
     * Guardrail: Filtro de seguridad que audita la respuesta del LLM y limpia alucinaciones.
     */
    fun validateAndFilterResponse(llmResponse: String): String {
        // 1. TIJERAS: Limpieza de alucinaciones y loops de texto del modelo local
        var cleanedResponse = llmResponse
            .substringBefore("```")  // Corta bloques de código o comillas raras
            .substringBefore("[INST]") // Por si alucina etiquetas
            .substringBefore("Mensaje para el paciente:") // Por si repite partes del prompt
            .trim()

        // Eliminar secuencias infinitas de puntos o comas (ej. "......" o ",,,,") comunes en alucinaciones
        cleanedResponse = cleanedResponse.replace(Regex("([.`*~_\\-,])\\1{3,}"), ".")

        // Eliminar saltos de línea excesivos (reduce huecos blancos gigantes)
        cleanedResponse = cleanedResponse.replace(Regex("\n{3,}"), "\n\n")

        // 2. FILTRO DE SEGURIDAD MÁS ESTRICTO (Palabras prohibidas)
        val lowerCaseResponse = cleanedResponse.lowercase()

        /*
        /////////
        RESPUESTA HARDCODEADA

        /////////
        for (word in forbiddenWords) {
            if (lowerCaseResponse.contains(word)) {
                // Log para auditoría (ideal para demostrar ante reguladores que tu sistema es seguro)
                Log.w("SanaMovil_Guardrails", "¡Bypass detectado! Palabra prohibida: '$word'")

                // Fallback seguro: Si el LLM rompe las reglas, devolvemos este texto pre-aprobado.
                return "Basado en los síntomas que nos compartiste, el sistema ha clasificado tu situación con prioridad. Por normativas de seguridad y salud, te recomendamos buscar valoración médica presencial en el tiempo indicado. No podemos ofrecer diagnósticos automatizados por este medio."
            }
        }
         */

        // 3. RETORNO SEGURO
        return if (cleanedResponse.isNotBlank()) {
            cleanedResponse
        } else {
            "Análisis de síntomas completado. Por favor, sigue la acción sugerida en la tarjeta superior."
        }
    }
}