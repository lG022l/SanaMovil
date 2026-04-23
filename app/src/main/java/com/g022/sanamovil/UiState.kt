package com.g022.sanamovil

import androidx.compose.ui.graphics.Color

enum class UserRole {
    OPERATOR,
    SUPERVISOR
}
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
    // Dentro de tu data class UiState, agrega esto:
    val wizardConsentAccepted: Boolean = false,

    val triageResult: com.g022.sanamovil.engine.TriageResult? = null,

    val currentRole: UserRole = UserRole.OPERATOR,
    val supervisorSelectedTab: Int = 0,

    // --- MÉTRICAS DEL DASHBOARD SUPERVISOR ---
    val dashTotalCasos: Int = 0,
    val dashTiempoPromedio: String = "0s",
    val dashRojos: Int = 0,
    val dashAmarillos: Int = 0,
    val dashVerdes: Int = 0,
    val dashOperadoresActivos: Int = 0
)

enum class EmergencyLevel(val color: Color, val label: String) {
    NONE(Color.Transparent, ""),
    LEVE(Color(0xFF4CAF50), "LEVE"), // Verde
    MODERADO(Color(0xFFFFC107), "MODERADO"), // Ambar/Amarillo
    SEVERO(Color(0xFFF44336), "SEVERO"), // Rojo
    EMERGENCIA(Color(0xFFFF0000), "EMERGENCIA 911") // Rojo Intenso
}