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
//import com.google.mediapipe.tasks.genai.llminference.LlmInference
//import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
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
import com.google.mediapipe.tasks.genai.llminference.LlmInference


// --- ACTIVITY PRINCIPAL ---
class MainActivity : ComponentActivity() {

    // Funciones nativas (JNI)
    external fun loadModel(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray): String

    // NUEVAS Funciones nativas (JNI) para Llama (MedGemma)
    external fun loadLlamaModel(modelPath: String): Boolean
    external fun generateTextLlama(prompt: String): String

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

    // Lógica de inicialización de Modelos
    suspend fun initModels(viewModel: SanaViewModel) = withContext(Dispatchers.IO) {
        // 1. Cargar Whisper
        val whisperPath = getModelPath("ggml-tiny.bin")
        if (File(whisperPath).exists()) {
            viewModel.isWhisperLoaded = loadModel(whisperPath)
        }

        // 2. Cargar Llama (MedGemma)
        // PON AQUÍ EL NOMBRE EXACTO DE TU ARCHIVO .GGUF:
        val llamaModelName = "medgemma-1.5-4b-it-Q4_K_M.gguf"
        val llamaPath = getModelPath(llamaModelName)

        if (File(llamaPath).exists()) {
            viewModel.isLlamaLoaded = loadLlamaModel(llamaPath)
            if(viewModel.isLlamaLoaded) {
                Log.d("SANA", "Cerebro Llama (MedGemma) cargado OK")
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
                        viewModel.setResult(
                            "LLAMA AL 911 INMEDIATAMENTE\n\n(Generando detalles clínicos...)",
                            EmergencyLevel.EMERGENCIA
                        )
                    }
                }

                // 2. Inferencia LLM
                if (viewModel.isLlamaLoaded) { // Usamos el nuevo flag del ViewModel
                    val prompt = buildPrompt(textoUsuario)

                    // Llamamos a nuestra nueva función de C++
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
        return "<start_of_turn>user\n" +
                "Actúa como un asistente médico de triaje y atención prehospitalaria. Tu objetivo es evaluar clínicamente los síntomas y proporcionar un plan de acción detallado.\n" +
                "\n" +
                "REGLAS:\n" +
                "1. Prioriza la seguridad y la estabilización del paciente.\n" +
                "2. Usa viñetas y listas numeradas para mayor claridad.\n" +
                "3. Usa emojis (🔴, ⚠️) para resaltar señales de alarma e información crítica.\n" +
                "4. Analiza EXCLUSIVAMENTE la información proporcionada por el paciente.\n" +
                "5. NO inventes síntomas ni datos que no se hayan mencionado.\n" +
                "6. Mantén un tono profesional, clínico y directo.\n" +
                "\n" +
                "Debes responder ESTRICTAMENTE con este formato:\n" +
                "NIVEL: [LEVE / MODERADO / EMERGENCIA]\n" +
                "[Diagnóstico principal o sospecha clínica]\n" +
                "\n" +
                "EVALUACIÓN:\n" +
                "- [Análisis de los síntomas y factores de riesgo presentados]\n" +
                "\n" +
                "PLAN RECOMENDADO:\n" +
                "1. [Pasos a seguir numerados, priorizando lo más urgente]\n" +
                "\n" +
                "🔴 SEÑALES DE ALARMA A VIGILAR:\n" +
                "- [Síntomas o signos vitales que indicarían un empeoramiento grave]\n" +
                "\n" +
                "⚠️ ADVERTENCIA: [Mensaje final de precaución o justificación de la urgencia]\n" +
                "\n" +
                "---\n" +
                "EJEMPLO:\n" +
                "Paciente: \"Paciente femenina, cuarta década de vida, embarazo de 11.2 semanas. Sangrado vaginal tipo manchado por 6 días. Dolor abdominal. Trabajo físico intenso durante la última semana. Orificio cervical externo cerrado.\"\n" +
                "Respuesta:\n" +
                "NIVEL: EMERGENCIA\n" +
                "Amenaza de aborto con factores de riesgo\n" +
                "\n" +
                "EVALUACIÓN:\n" +
                "- Sangrado vaginal de 6 días en primer trimestre con dolor abdominal\n" +
                "- Trabajo físico intenso = factor de riesgo para aborto incompleto\n" +
                "- Edad materna >35 = factor de riesgo adicional\n" +
                "\n" +
                "PLAN RECOMENDADO:\n" +
                "1. Monitoreo cada 2-4 horas (signos vitales + cantidad de sangrado)\n" +
                "2. Cuantificar sangrado: número de toallas sanitarias/hora\n" +
                "3. Establecer acceso venoso periférico preventivo\n" +
                "4. Preparar plan de traslado de emergencia AHORA\n" +
                "   - Identificar vehículo disponible\n" +
                "   - Contactar hospital receptor si hay señal\n" +
                "   - Tener líquidos IV listos para transporte\n" +
                "\n" +
                "🔴 SEÑALES DE ALARMA A VIGILAR:\n" +
                "- Sangrado que empapa >1 toalla/hora\n" +
                "- Taquicardia >100 lpm o PA sistólica <90 mmHg\n" +
                "- Mareo, palidez, pérdida de consciencia\n" +
                "- Fiebre >38°C\n" +
                "\n" +
                "⚠️ ADVERTENCIA: Con sangrado de 6 días y dolor progresivo, el riesgo de evolución a aborto incompleto con hemorragia es SIGNIFICATIVO. No esperar a que sea emergencia para planear traslado. Preparar logística de transporte inmediatamente.\n" +
                "---\n" +
                "\n" +
                "Paciente: \"$textoUsuario\"<end_of_turn>\n" +
                "<start_of_turn>model\n" +
                "Respuesta:\n"
    }
}

