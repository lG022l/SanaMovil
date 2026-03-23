package com.g022.sanamovil.engine

// 1. Definimos los niveles de riesgo estándar de triaje (Base)
enum class RiskLevel(val priority: Int, val description: String) {
    RED(1, "Emergencia - Riesgo vital inmediato, atención al instante"),
    ORANGE(2, "Muy Urgente - Riesgo vital potencial, atención en < 15 min"),
    YELLOW(3, "Urgencia - Condición estable pero requiere atención en < 60 min"),
    GREEN(4, "Urgencia Menor - Situación no de riesgo, atención en < 120 min"),
    BLUE(5, "No Urgente - Problema clínico menor, atención en < 240 min")
}

// 2. Definimos la estructura exacta extraída de Whisper + Wizard
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
    val radiatingPain: Boolean = false, // Si el dolor se irradia
    val hasSevereBleeding: Boolean = false,
    val hasBreathingDifficulty: Boolean = false,
    val hasHighFever: Boolean = false
)

// ==============================================================
// TODO LO NUEVO DE LA FASE 9 (Modelos de Salida para Auditoría)
// ==============================================================

// 3. Nuevos Enums para la acción y urgencia logística
enum class UrgencyLevel(val title: String) {
    IMMEDIATE("Prioridad Alta - Inmediata"),
    URGENT("Prioridad Media - Urgente"),
    ROUTINE("Prioridad Baja - Rutina")
}

enum class ActionType(val label: String) {
    TRANSFER("Traslado a Urgencias"),
    CONSULT("Consulta Médica"),
    MONITOR("Monitoreo en Casa")
}

// 4. El nuevo modelo que agrupa toda la respuesta final
data class TriageResult(
    val urgencyLevel: UrgencyLevel,
    val timeframe: String,
    val actionType: ActionType,
    val standardMessage: String,
    val llmExplanation: String,
    val disclaimer: String = "⚠️ AVISO LEGAL: Esta es una herramienta de priorización logística basada en algoritmos, NO es un diagnóstico médico definitivo. Si sientes que tu vida corre peligro, llama al 911 o acude a urgencias inmediatamente.",

    // Trazabilidad y Versión (Crítico para auditoría legal)
    val triggeredRules: List<String> = emptyList(),
    val ruleEngineVersion: String = "v1.0.0-determinista",
    val llmModelVersion: String = "MedGemma-2B-local"
)

// 5. Biblioteca de Respuestas Estandarizadas
object ResponseLibrary {
    fun mapRiskToUrgency(riskLevel: RiskLevel): UrgencyLevel {
        return when (riskLevel) {
            RiskLevel.RED, RiskLevel.ORANGE -> UrgencyLevel.IMMEDIATE
            RiskLevel.YELLOW -> UrgencyLevel.URGENT
            RiskLevel.GREEN, RiskLevel.BLUE -> UrgencyLevel.ROUTINE
        }
    }

    fun getStandardMessage(urgency: UrgencyLevel): String {
        return when(urgency) {
            UrgencyLevel.IMMEDIATE -> "Traslado inmediato al centro de salud u hospital más cercano. No permanecer solo."
            UrgencyLevel.URGENT -> "Buscar atención médica en las próximas 4 a 24 horas. Guardar reposo."
            UrgencyLevel.ROUTINE -> "Monitorear síntomas. Reevaluar si empeoran. Agendar consulta programada."
        }
    }

    fun getTimeframe(urgency: UrgencyLevel): String {
        return when(urgency) {
            UrgencyLevel.IMMEDIATE -> "En las próximas 2 horas"
            UrgencyLevel.URGENT -> "En las próximas 4 a 24 horas"
            UrgencyLevel.ROUTINE -> "En los próximos días (No urgente)"
        }
    }

    fun getActionType(urgency: UrgencyLevel): ActionType {
        return when(urgency) {
            UrgencyLevel.IMMEDIATE -> ActionType.TRANSFER
            UrgencyLevel.URGENT -> ActionType.CONSULT
            UrgencyLevel.ROUTINE -> ActionType.MONITOR
        }
    }
}