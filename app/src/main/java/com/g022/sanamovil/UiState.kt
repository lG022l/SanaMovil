package com.g022.sanamovil

import androidx.compose.ui.graphics.Color

// Estados de la UI
data class UiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String = "Iniciando sistemas...",
    val analysisResult: String = "",
    val emergencyLevel: EmergencyLevel = EmergencyLevel.NONE,

    // --- NUEVAS VARIABLES PARA LA FASE 8 (WIZARD) ---
    val showWizard: Boolean = false, // Controla si se muestra la pantalla de preguntas

    // Campos obligatorios del Wizard
    val wizardAge: String = "", // Usamos String para los TextFields, luego convertimos a Int
    val wizardDuration: String = "Minutos", // Opciones: Minutos, Horas, Días
    val wizardIntensity: Float = 5f, // Slider de 1 a 10

    // Campos condicionales dinámicos
    val askAboutConsciousness: Boolean = false,
    val hasLossOfConsciousness: Boolean = false,

    val askAboutRadiation: Boolean = false,
    val hasRadiatingPain: Boolean = false,

    val triageResult: com.g022.sanamovil.engine.TriageResult? = null,
)

enum class EmergencyLevel(val color: Color, val label: String) {
    NONE(Color.Transparent, ""),
    LEVE(Color(0xFF4CAF50), "LEVE"), // Verde
    MODERADO(Color(0xFFFFC107), "MODERADO"), // Ambar/Amarillo
    SEVERO(Color(0xFFF44336), "SEVERO"), // Rojo
    EMERGENCIA(Color(0xFFFF0000), "EMERGENCIA 911") // Rojo Intenso
}