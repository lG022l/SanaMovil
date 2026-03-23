
package com.g022.sanamovil.engine

// 1. Definimos los niveles de riesgo estándar de triaje
enum class RiskLevel(val priority: Int, val description: String) {
    RED(1, "Emergencia - Riesgo vital inmediato, atención al instante"),
    ORANGE(2, "Muy Urgente - Riesgo vital potencial, atención en < 15 min"),
    YELLOW(3, "Urgencia - Condición estable pero requiere atención en < 60 min"),
    GREEN(4, "Urgencia Menor - Situación no de riesgo, atención en < 120 min"),
    BLUE(5, "No Urgente - Problema clínico menor, atención en < 240 min")
}

// 2. Definimos la estructura exacta que intentaremos extraer de Whisper
data class StructuredSymptoms(
    val age: Int? = null,
    val durationHours: Int? = null,
    val intensity: Int? = null, // Escala del 1 al 10
    val relevantHistory: List<String> = emptyList(),

    // Banderas clínicas generales
    val isConscious: Boolean = true,
    val associatedSymptoms: List<String> = emptyList(),

    // Banderas rojas específicas (Red Flags) para el motor determinista
    val hasChestPain: Boolean = false,
    val radiatingPain: Boolean = false, // Si el dolor se irradia (ej. brazo, mandíbula)
    val hasSevereBleeding: Boolean = false,
    val hasBreathingDifficulty: Boolean = false,
    val hasHighFever: Boolean = false
)