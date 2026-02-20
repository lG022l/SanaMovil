package com.g022.sanamovil

import androidx.compose.ui.graphics.Color

// Estados de la UI
data class UiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String = "Iniciando sistemas...",
    val analysisResult: String = "",
    val emergencyLevel: EmergencyLevel = EmergencyLevel.NONE
)

enum class EmergencyLevel(val color: Color, val label: String) {
    NONE(Color.Transparent, ""),
    LEVE(Color(0xFF4CAF50), "LEVE"), // Verde
    MODERADO(Color(0xFFFFC107), "MODERADO"), // Ambar/Amarillo
    SEVERO(Color(0xFFF44336), "SEVERO"), // Rojo
    EMERGENCIA(Color(0xFFFF0000), "EMERGENCIA 911") // Rojo Intenso
}