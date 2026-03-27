package com.g022.sanamovil

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.g022.sanamovil.Auth.LoginScreen
import com.g022.sanamovil.Auth.RegisterScreen
import com.g022.sanamovil.Home.SanaAppScreen
import com.g022.sanamovil.Theme.SanaAppTheme
import com.g022.sanamovil.ViewModel.SanaViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// --- ACTIVITY PRINCIPAL ---
class MainActivity : ComponentActivity() {

    // Funciones nativas (JNI)
    external fun loadModel(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray): String

    // NUEVAS Funciones nativas (JNI) para Llama (MedGemma)
    external fun loadLlamaModel(modelPath: String): Boolean
    external fun generateTextLlama(prompt: String): String

    // Función nativa para el streaming (Paso 2 y 3)
    external fun generateTextLlamaStream(prompt: String, callback: com.g022.sanamovil.engine.LlamaStreamCallback)

    companion object {
        init {
            System.loadLibrary("sanamovil")
            System.loadLibrary("sana_llama")
        }
    }

    private val triggersEmergencia = listOf(
        "infarto", "paro", "corazón", "arritmia", "asfixia", "ahogo", "no respira", "azul",
        "desmayo", "inconsciente", "convulsion", "derrame", "acv", "despierta",
        "hemorragia", "sangrado", "sangre", "baleado", "disparo", "puñalada", "cuchillo", "quemadura",
        "suicidio", "matarme", "veneno", "brazo izquierdo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SanaAppTheme {

                // 1. Iniciamos el controlador de navegación
                val navController = rememberNavController()

                // 2. Configuramos las rutas
                NavHost(
                    navController = navController,
                    startDestination = "login_screen"
                ) {

                    // Ruta 1: Pantalla de Login
                    composable("login_screen") {
                        LoginScreen(
                            onLoginClick = { email, password ->
                                if (email == "admin" && password == "1234") {
                                    navController.navigate("home_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                }
                            },
                            onRegisterClick = { navController.navigate("registro_screen") },
                            onForgotPasswordClick = { /* Pendiente */ }
                        )
                    }

                    // Ruta 2: Pantalla de Registro
                    composable("registro_screen") {
                        RegisterScreen(
                            onRegisterClick = { correo, pass ->
                                navController.navigate("login_screen") {
                                    popUpTo("login_screen") { inclusive = true }
                                }
                            },
                            onBackToLogin = {
                                navController.popBackStack()
                            }
                        )
                    }


                    // Ruta 3: Pantalla Principal de la Demo (SanaAppScreen)
                    composable("home_screen") {
                        SanaAppScreen(
                            onRecordRequest = { duration, callback -> grabarYProcesarAudio(duration, callback) },
                            onAnalyzeRequest = { text, viewModel -> procesarTexto(text, viewModel) },
                            activityContext = this@MainActivity
                        )
                    }
                }
            }
        }
    }

    // Lógica de inicialización de Modelos
    suspend fun initModels(viewModel: SanaViewModel) = withContext(Dispatchers.IO) {
        // 1. Cargar Whisper
        val whisperPath = getModelPath("ggml-tiny.bin")
        if (File(whisperPath).exists()) {
            viewModel.isWhisperLoaded = loadModel(whisperPath)
        }

        // 2. Cargar Llama (MedGemma)
        val llamaModelName = "medgemma-1.5-4b-it-Q4_K_M.gguf"
        val llamaPath = getModelPath(llamaModelName)

        if (File(llamaPath).exists()) {
            viewModel.isLlamaLoaded = loadLlamaModel(llamaPath)
            if(viewModel.isLlamaLoaded) {
                Log.d("SANA", "Cerebro Llama (MedGemma) cargado OK")

                // --- ACTUALIZACIÓN PASO 3: Conectamos la función de Streaming al ViewModel ---
                viewModel.generateLlamaStream = { prompt, onTokenGenerated ->
                    generateTextLlamaStream(prompt, object : com.g022.sanamovil.engine.LlamaStreamCallback {
                        override fun onToken(token: String) {
                            onTokenGenerated(token)
                        }
                    })
                }

                // Mantenemos también la función clásica por si `procesarTexto` (legacy) la sigue usando
                viewModel.generateLlamaResponse = { prompt ->
                    generateTextLlama(prompt)
                }

            } else {
                Log.e("SANA", "Falló la carga de Llama en C++")
            }
        } else {
            Log.e("SANA", "No se encontró el archivo del modelo en assets")
        }
    }

    private fun procesarTexto(textoUsuario: String, viewModel: SanaViewModel) {
        viewModel.setLoading(true, "Analizando gravedad... ")
        viewModel.updateInput("") // Limpiar input

        Thread {
            try {
                // 1. Detección de palabras clave
                val esEmergencia = triggersEmergencia.any { textoUsuario.lowercase().contains(it) }

                if (esEmergencia) {
                    runOnUiThread {
                        val resultadoFase9 = com.g022.sanamovil.engine.TriageResult(
                            urgencyLevel = com.g022.sanamovil.engine.UrgencyLevel.ROUTINE,
                            timeframe = "Evaluación preliminar",
                            actionType = com.g022.sanamovil.engine.ActionType.MONITOR,
                            standardMessage = "Análisis generado por el motor.",
                            llmExplanation = "LLAMA AL 911 INMEDIATAMENTE\n\n(Generando detalles clínicos...)",
                            triggeredRules = emptyList()
                        )
                        viewModel.setResult(resultadoFase9, EmergencyLevel.NONE)
                    }
                }

                // 2. Inferencia LLM (Nota: Esta función legacy sigue usando la generación de 1 solo bloque)
                if (viewModel.isLlamaLoaded) {
                    val prompt = buildPrompt(textoUsuario)
                    val respuestaIA = generateTextLlama(prompt)

                    runOnUiThread {
                        determinarNivelYMostrar(respuestaIA, esEmergencia, textoUsuario, viewModel)
                    }
                } else {
                    runOnUiThread { viewModel.setLoading(false, "Error: Modelo Llama no cargado") }
                }

            } catch (e: Exception) {
                runOnUiThread { viewModel.setLoading(false, "Error: ${e.message}") }
            }
        }.start()
    }

    private fun grabarYProcesarAudio(durationSecs: Int, onResult: (String) -> Unit) {
        Thread {
            val audioData = grabarAudio(durationSecs)
            if (audioData.isNotEmpty()) {
                val texto = transcribeAudio(audioData)
                onResult(texto)
            }
        }.start()
    }

    private fun determinarNivelYMostrar(respuestaIA: String, esEmergenciaPrevia: Boolean, textoUsuario: String, viewModel: SanaViewModel) {
        val respuestaNorm = respuestaIA.uppercase()
        var nivel = EmergencyLevel.LEVE

        if (esEmergenciaPrevia) {
            nivel = EmergencyLevel.EMERGENCIA
        } else {
            if (respuestaNorm.contains("EMERGENCIA")) {
                nivel = EmergencyLevel.EMERGENCIA
            } else if (respuestaNorm.contains("SEVERO") || respuestaNorm.contains("(ROJO)")) {
                nivel = EmergencyLevel.SEVERO
            } else if (respuestaNorm.contains("MODERADO") || respuestaNorm.contains("(AMARILLO)")) {
                nivel = EmergencyLevel.MODERADO
            }
        }

        val textoFinal = if (esEmergenciaPrevia) {
            "SÍNTOMAS: $textoUsuario\n\nDETECTADO POSIBLE RIESGO VITAL\n\nAnálisis IA:\n$respuestaIA"
        } else {
            "SÍNTOMAS: $textoUsuario\n\n$respuestaIA".replace("Respuesta:", "").trim()
        }

        val resultadoEmpaquetado = com.g022.sanamovil.engine.TriageResult(
            urgencyLevel = com.g022.sanamovil.engine.UrgencyLevel.URGENT,
            timeframe = "Evaluación en proceso",
            actionType = com.g022.sanamovil.engine.ActionType.CONSULT,
            standardMessage = "Análisis generado por el modelo local.",
            llmExplanation = textoFinal,
            triggeredRules = listOf("Análisis de texto libre heredado")
        )

        viewModel.setResult(resultadoEmpaquetado, nivel)
    }

    private fun grabarAudio(durationSecs: Int): FloatArray {
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return FloatArray(0)

        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return FloatArray(0)

        val data = ShortArray(sampleRate * durationSecs)
        recorder.startRecording()
        recorder.read(data, 0, data.size)
        recorder.stop()
        recorder.release()

        return FloatArray(data.size) { i -> data[i] / 32768.0f }
    }

    private fun getModelPath(assetName: String): String {
        val file = File(filesDir, assetName)
        if (!file.exists()) {
            try {
                assets.open("models/$assetName").use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("SANA", "Error asset: $e")
            }
        }
        return file.absolutePath
    }

    private fun buildPrompt(textoUsuario: String): String {
        return """
        <start_of_turn>user
        Como médico de triaje prehospitalario, evalúa basándote SOLO en los datos provistos. Prioriza la estabilización. Se lo más conciso posible.  Responde ESTRICTAMENTE con esta estructura exacta:

        NIVEL: [LEVE / MODERADO / EMERGENCIA]
        POSIBLE DIAGNOSTICO:
        [Diagnóstico principal]

        EVALUACIÓN:
        - [Análisis clínico breve]

        PLAN RECOMENDADO:
        1. [Pasos urgentes, máximo 5]

        🔴 SEÑALES DE ALARMA A VIGILAR:
        - [Signos de empeoramiento]

        ⚠️ ADVERTENCIA: [Riesgo principal]

        Paciente: "$textoUsuario"<end_of_turn>
        <start_of_turn>model
        
    """.trimIndent()
    }
}