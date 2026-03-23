package com.g022.sanamovil.engine

class ClinicalRuleEngine {

    /**
     * Calcula el nivel de riesgo basado en un árbol de decisión determinista.
     * Fuentes clínicas de referencia:
     * - Protocolo START (Simple Triage and Rapid Treatment)
     * - Sistema de Triage Manchester (MTS - Adaptación General)
     */
    fun evaluateSymptoms(symptoms: StructuredSymptoms): RiskLevel {

        // REGLA 1: ROJO (Emergencia Vital - Atención Inmediata)
        // Fuente Clínica: Protocolo START / Soporte Vital Básico
        // Justificación: Vía aérea comprometida, shock hemorrágico o alteración severa del estado de alerta.
        if (!symptoms.isConscious || symptoms.hasSevereBleeding || symptoms.hasBreathingDifficulty) {
            return RiskLevel.RED
        }

        // REGLA 2: NARANJA (Muy Urgente - < 15 min)
        // Fuente Clínica: Guías AHA (American Heart Association) para dolor torácico / Escala analgésica visual (EVA > 8)
        // Justificación: Posible Síndrome Coronario Agudo (IAM) o dolor insoportable.
        if ((symptoms.hasChestPain && symptoms.radiatingPain) || (symptoms.intensity ?: 0) >= 9) {
            return RiskLevel.ORANGE
        }

        // REGLA 3: AMARILLO (Urgencia - < 60 min)
        // Fuente Clínica: MTS / Infección con respuesta sistémica o dolor moderado-severo.
        if (symptoms.hasHighFever || (symptoms.intensity ?: 0) in 6..8) {
            return RiskLevel.YELLOW
        }

        // REGLA 4: VERDE (Urgencia Menor - < 120 min)
        // Justificación: Dolor leve a moderado, sin signos de compromiso vital.
        if ((symptoms.intensity ?: 0) in 3..5) {
            return RiskLevel.GREEN
        }

        // REGLA 5: AZUL (No Urgente - < 240 min)
        // Justificación: Síntomas leves, revisión de rutina o condiciones crónicas estables.
        return RiskLevel.BLUE
    }
}