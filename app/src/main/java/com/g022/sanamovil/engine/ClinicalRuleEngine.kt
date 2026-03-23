package com.g022.sanamovil.engine

// Nueva clase para devolver el resultado + la auditoría
data class EngineEvaluation(
    val riskLevel: RiskLevel,
    val triggeredRules: List<String>
)

class ClinicalRuleEngine {

    /**
     * Calcula el nivel de riesgo y documenta qué reglas exactas se activaron.
     */
    fun evaluateSymptoms(symptoms: StructuredSymptoms): EngineEvaluation {
        val rules = mutableListOf<String>()

        // REGLA 1: ROJO (Emergencia Vital - Atención Inmediata)
        if (!symptoms.isConscious || symptoms.hasSevereBleeding || symptoms.hasBreathingDifficulty) {
            if (!symptoms.isConscious) rules.add("Regla START: Paciente inconsciente o alteración severa del estado de alerta.")
            if (symptoms.hasSevereBleeding) rules.add("Regla Hemorragia: Sangrado severo reportado.")
            if (symptoms.hasBreathingDifficulty) rules.add("Regla Vía Aérea: Dificultad respiratoria crítica.")

            return EngineEvaluation(RiskLevel.RED, rules)
        }

        // REGLA 2: NARANJA (Muy Urgente - < 15 min)
        if ((symptoms.hasChestPain && symptoms.radiatingPain) || (symptoms.intensity ?: 0) >= 9) {
            if (symptoms.hasChestPain && symptoms.radiatingPain) rules.add("Regla AHA: Dolor torácico con irradiación (Posible isquemia).")
            if ((symptoms.intensity ?: 0) >= 9) rules.add("Regla EVA: Dolor de intensidad extrema (>=9).")

            return EngineEvaluation(RiskLevel.ORANGE, rules)
        }

        // REGLA 3: AMARILLO (Urgencia - < 60 min)
        if (symptoms.hasHighFever || (symptoms.intensity ?: 0) in 6..8) {
            if (symptoms.hasHighFever) rules.add("Regla Infección: Fiebre alta con posible respuesta sistémica.")
            if ((symptoms.intensity ?: 0) in 6..8) rules.add("Regla EVA: Dolor moderado-severo (6-8).")

            return EngineEvaluation(RiskLevel.YELLOW, rules)
        }

        // REGLA 4: VERDE (Urgencia Menor - < 120 min)
        if ((symptoms.intensity ?: 0) in 3..5) {
            rules.add("Regla EVA: Dolor leve a moderado (3-5) sin signos de compromiso vital.")
            return EngineEvaluation(RiskLevel.GREEN, rules)
        }

        // REGLA 5: AZUL (No Urgente - < 240 min)
        rules.add("Regla Base: Síntomas leves o estables. Ausencia de banderas rojas.")
        return EngineEvaluation(RiskLevel.BLUE, rules)
    }
}