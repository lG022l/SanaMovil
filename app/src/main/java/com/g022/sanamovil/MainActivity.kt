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
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
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

    companion object {
        init { System.loadLibrary("sanamovil") }
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
                        // Llama a tu función LoginScreen (asegúrate de importarla si está en otro paquete)
                        LoginScreen(
                            onLoginClick = { email, password ->
                                if (email == "admin" && password == "1234") {
                                    // Si las credenciales son correctas, navega a la demo
                                    navController.navigate("home_screen") {
                                        // Esto evita que al presionar 'Atrás' el usuario vuelva al Login
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
                                    // Por ser demo, si le da registrar lo mandamos directo al login o a la app
                                    navController.navigate("login_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                },
                                onBackToLogin = {
                                    // Esto lo regresa a la pantalla anterior (el login)
                                    navController.popBackStack()
                                }
                            )
                        }


                    // Ruta 3: Pantalla Principal de la Demo (SanaAppScreen)
                    composable("home_screen") {
                        // Aquí llamamos a tu demo original, pasándole las funciones que necesita
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

    // Lógica de inicialización de Modelos (movida a Corrutina en la UI para simplificar)
    suspend fun initModels(viewModel: SanaViewModel) = withContext(Dispatchers.IO) {
        val whisperPath = getModelPath("ggml-tiny.bin")
        if (File(whisperPath).exists()) {
            viewModel.isWhisperLoaded = loadModel(whisperPath)
        }

        val modelName = "gemma-2b-it-cpu-int4.bin"
        val modelFile = File(filesDir, modelName)

        if (modelFile.exists()) {
            try {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1500)
                    .setMaxTopK(40)
                    .build()
                viewModel.cerebroIA = LlmInference.createFromOptions(this@MainActivity, options)
                Log.d("SANA", "Cerebro cargado OK")
            } catch (e: Exception) {
                Log.e("SANA", "Error MediaPipe: ${e.message}")
            }
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
                        viewModel.setResult(
                            "LLAMA AL 911 INMEDIATAMENTE\n\n(Generando detalles clínicos...)",
                            EmergencyLevel.EMERGENCIA
                        )
                    }
                }

                // 2. Inferencia LLM
                if (viewModel.cerebroIA != null) {
                    val prompt = buildPrompt(textoUsuario)
                    val respuestaIA = viewModel.cerebroIA!!.generateResponse(prompt)

                    runOnUiThread {
                        determinarNivelYMostrar(respuestaIA, esEmergencia, textoUsuario, viewModel)
                    }
                } else {
                    runOnUiThread { viewModel.setLoading(false, "Error: IA no disponible") }
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
                onResult(texto) // Devuelve el texto al hilo principal o ViewModel
            }
        }.start()
    }

    private fun determinarNivelYMostrar(respuestaIA: String, esEmergenciaPrevia: Boolean, textoUsuario: String, viewModel: SanaViewModel) {
        val respuestaNorm = respuestaIA.uppercase()
        var nivel = EmergencyLevel.LEVE

        if (esEmergenciaPrevia) {
            nivel = EmergencyLevel.EMERGENCIA
        } else {
            if (respuestaNorm.contains("(ROJO)") || respuestaNorm.contains("SEVERO")) nivel = EmergencyLevel.SEVERO
            else if (respuestaNorm.contains("(AMARILLO)") || respuestaNorm.contains("MODERADO")) nivel = EmergencyLevel.MODERADO
        }

        val textoFinal = if (esEmergenciaPrevia) {
            "SÍNTOMAS: $textoUsuario\n\nDETECTADO POSIBLE RIESGO VITAL\n\nAnálisis IA:\n$respuestaIA"
        } else {
            "SÍNTOMAS: $textoUsuario\n\n$respuestaIA".replace("Respuesta:", "").trim()
        }

        viewModel.setResult(textoFinal, nivel)
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
        // Tu prompt original intacto
        return "<start_of_turn>user\n" +
                "Actúa como un asistente de triaje médico de emergencia. Tu objetivo es clasificar el síntoma rápidamente.\n" +
                "\n" +
                "REGLAS:\n" +
                "1. Sé breve y directo.\n" +
                "2. Prioriza la seguridad.\n" +
                "3. Usa emojis para visualización rápida.\n" +
                "4. Analiza EXCLUSIVAMENTE lo que el paciente escribe.\n" +
                "5. NO inventes síntomas.\n" +
                "6. NO copies los ejemplos.\n" +
                "\n" +
                "Debes responder ESTRICTAMENTE con este formato:\n" +
                "NIVEL: [LEVE / MODERADO / EMERGENCIA]\n" +
                "SOSPECHA: [1 o 2 palabras clave]\n" +
                "ACCIÓN: [La recomendación más importante]\n" +
                "\n" +
                "---\n" +
                "EJEMPLO 1:\n" +
                "Paciente: \"Me duele el pecho y el brazo izquierdo, sudo frío.\"\n" +
                "Respuesta:\n" +
                "NIVEL: EMERGENCIA\n" +
                "SOSPECHA: Infarto Cardíaco\n" +
                "ACCIÓN: Llamar a emergencias YA. No moverse.\n" +
                "\n" +
                "EJEMPLO 2:\n" +
                "Paciente: \"Me torcí el tobillo, duele un poco pero puedo caminar.\"\n" +
                "Respuesta:\n" +
                "NIVEL: LEVE\n" +
                "SOSPECHA: Esguince leve\n" +
                "ACCIÓN: Hielo y reposo. Si empeora, ir al médico.\n" +
                "\n" +
                "EJEMPLO 3:\n" +
                "Paciente: \"Tengo media cara paralizada y no puedo hablar bien de la nada.\"\n" +
                "Respuesta:\n" +
                "NIVEL: EMERGENCIA\n" +
                "SOSPECHA: ACV / Ictus\n" +
                "ACCIÓN: Correr a urgencias inmediatamente (Código Ictus).\n" +
                "\n" +
                "EJEMPLO 4:\n" +
                "Paciente: \"Tengo flemas en la garganta y fiebre de 38.\"\n" +
                "Respuesta:\n" +
                "NIVEL: MODERADO\n" +
                "SOSPECHA: Amigdalitis bacteriana\n" +
                "ACCIÓN: Ir al médico para valoración de antibióticos.\n" +
                "\n" +
                "EJEMPLO 5:\n" +
                "Paciente: \"Tengo irritada la piel por tocar una planta.\"\n" +
                "Respuesta:\n" +
                "NIVEL: LEVE\n" +
                "SOSPECHA: Dermatitis de contacto\n" +
                "ACCIÓN: Lavar con agua y jabón. Crema hidratante.\n" +
                "---\n" +
                "\n" +
                "Paciente: \"$textoUsuario\"<end_of_turn>\n" +
                "<start_of_turn>model\n" +
                "Respuesta:";


    }
}

