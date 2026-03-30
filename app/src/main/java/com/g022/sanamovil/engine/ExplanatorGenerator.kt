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
     * Crea el prompt estricto para que el LLM genere la explicación basada en la decisión previa.
     */
    fun buildExplanationPrompt(symptoms: StructuredSymptoms, riskLevel: RiskLevel): String {
        return """
            [INST]
            Eres un asistente de orientación empático para la aplicación médica. Tu tarea es explicar brevemente por qué el sistema asignó este nivel de prioridad.
            
            DATOS CALCULADOS:
            - Nivel de Riesgo: ${riskLevel.name} (${riskLevel.description})
            - Dolor reportado: ${symptoms.intensity ?: "No especificado"}/10
            - Fiebre alta: ${if(symptoms.hasHighFever) "Sí" else "No"}
            - Dificultad respiratoria: ${if(symptoms.hasBreathingDifficulty) "Sí" else "No"}
            - Dolor de pecho: ${if(symptoms.hasChestPain) "Sí" else "No"}
            
            REGLAS:
            1. NUNCA des un diagnóstico médico ni menciones enfermedades.
            2. NUNCA sugieras medicamentos.
            3. Escribe máximo 2 párrafos empáticos.
            
            Tu respuesta debe ser ÚNICAMENTE el mensaje dirigido al paciente. NO escribas más reglas. NO repitas mis instrucciones.
            [/INST]
            
            Mensaje para el paciente:
        """.trimIndent()
    }

    /**
     * Guardrail: Filtro de seguridad que audita la respuesta del LLM.
     */
    fun validateAndFilterResponse(llmResponse: String): String {
        val lowerCaseResponse = llmResponse.lowercase()

        for (word in forbiddenWords) {
            if (lowerCaseResponse.contains(word)) {
                // Log para auditoría (ideal para demostrar ante reguladores que tu sistema es seguro)
                Log.w("SanaMovil_Guardrails", "¡Bypass detectado! Palabra prohibida: '$word'")

                // Fallback seguro: Si el LLM rompe las reglas, devolvemos este texto pre-aprobado.
                return "Basado en los síntomas que nos compartiste, el sistema ha clasificado tu situación con prioridad. Por normativas de seguridad y salud, te recomendamos buscar valoración médica presencial en el tiempo indicado. No podemos ofrecer diagnósticos automatizados por este medio."
            }
        }

        // Si pasa la validación, devolvemos la respuesta original del LLM
        return llmResponse
    }
}