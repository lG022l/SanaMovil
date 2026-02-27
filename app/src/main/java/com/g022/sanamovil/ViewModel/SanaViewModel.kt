package com.g022.sanamovil.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.g022.sanamovil.EmergencyLevel
import com.g022.sanamovil.UiState
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class SanaViewModel : ViewModel() {
    var uiState by mutableStateOf(UiState())
        private set

    // var cerebroIA: LlmInference? = null

    // AGREGA ESTA NUEVA LÍNEA:
    var isLlamaLoaded = false
    var isWhisperLoaded = false

    // Historial ficticio para el menú lateral
    var recentQueries = mutableStateListOf<String>()

    fun updateInput(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun setLoading(isLoading: Boolean, message: String = "") {
        uiState = uiState.copy(isLoading = isLoading, statusMessage = message)
    }

    fun setResult(result: String, level: EmergencyLevel) {
        uiState = uiState.copy(
            analysisResult = result,
            emergencyLevel = level,
            isLoading = false
        )
        // Agregar al historial si hay un resultado válido
        if (result.isNotEmpty()) {
            val preview = result.take(30).replace("\n", " ") + "..."
            recentQueries.add(0, preview)
        }
    }
}