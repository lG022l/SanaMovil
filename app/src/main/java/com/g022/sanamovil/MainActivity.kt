package com.g022.sanamovil

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions

class MainActivity : AppCompatActivity() {

    private lateinit var btnRecord: Button
    private lateinit var tvResult: TextView
    private var isWhisperLoaded = false
    private var cerebroIA: LlmInference? = null

    private val triggersEmergencia = listOf(
        "infarto", "paro", "corazón", "arritmia",

        // RESPIRATORIO
        "asfixia", "ahogo", "no respira", "azul", // azul por cianosis

        // NEUROLÓGICO
        "desmayo", "inconsciente", "convulsion", "derrame", "acv", "despierta", // "no despierta"

        // TRAUMA / SANGRE
        "hemorragia", "sangrado", "sangre", "baleado", "disparo", "puñalada", "cuchillo", "quemadura",

        // CRÍTICO / OTROS
        "suicidio", "matarme", "veneno"
    )

    // ===== WHISPER (C++) =====
    external fun loadModel(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray): String

    companion object {
        init { System.loadLibrary("sanamovil") }
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        tvResult = findViewById(R.id.tvResult)
        window.decorView.setBackgroundColor(0xFF1A1A1A.toInt())

        tvResult.setTextColor(0xFFFFFFFF.toInt()) // Texto Blanco
        tvResult.textSize = 18f
        btnRecord.isEnabled = false
        tvResult.text = "Iniciando sistemas..."

        Thread {
            // 1. CARGAR WHISPER
            val whisperPath = getModelPath("ggml-tiny.bin")
            if (File(whisperPath).exists()) {
                isWhisperLoaded = loadModel(whisperPath)
            }

            // 2. CARGAR GEMMA (MediaPipe)
            val modelName = "gemma-2b-it-cpu-int4.bin"
            val modelFile = File(filesDir, modelName)

            if (modelFile.exists()) {
                try {
                    // Configuración básica y estable
                    val options = LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(512)
                        .build()

                    cerebroIA = LlmInference.createFromOptions(this, options)
                    Log.d("SANA", "Cerebro cargado OK")
                } catch (e: Exception) {
                    Log.e("SANA", "Error MediaPipe: ${e.message}")
                }
            }

            runOnUiThread {
                if (isWhisperLoaded) {
                    btnRecord.isEnabled = true
                    val estado = if (cerebroIA != null) "Cerebro ACTIVO 🧠" else "Cerebro OFF ❌"
                    tvResult.text = "Whisper listo 👂\n$estado\n\nPresiona GRABAR"
                    window.decorView.setBackgroundColor(0xFF000000.toInt()) // Negro inicial
                } else {
                    tvResult.text = "Error: Whisper no cargó"
                }
            }
        }.start()

        btnRecord.setOnClickListener {
            if (checkPermissions()) iniciarGrabacion()
            else requestPermissions()
        }
    }

    private fun iniciarGrabacion() {
        btnRecord.isEnabled = false
        btnRecord.text = "Escuchando..."

        runOnUiThread {
            window.decorView.setBackgroundColor(0xFF000000.toInt())
            tvResult.text = "Grabando... 🎙️"
            tvResult.setTextColor(0xFFFFFFFF.toInt())
        }

        Thread {
            try {
                // 1. GRABAR
                val audioData = grabarAudio(3)
                if (audioData.isEmpty()) return@Thread

                runOnUiThread { tvResult.text = "Transcribiendo... 📝" }

                // 2. TRANSCRIBIR
                val textoUsuario = transcribeAudio(audioData)
                Log.d("SANA", "Usuario: $textoUsuario")

                // --- BYPASS DE SEGURIDAD INMEDIATO ---
                // Verificamos YA MISMO si es emergencia, sin esperar a la IA
                val esEmergenciaDetectada = triggersEmergencia.any { textoUsuario.lowercase().contains(it) }

                if (esEmergenciaDetectada) {
                    // ¡ACCIÓN INMEDIATA! No esperamos a la IA para alertar
                    runOnUiThread {
                        window.decorView.setBackgroundColor(0xFFFF4444.toInt()) // ROJO PURO
                        tvResult.text = "🚨 ¡POSIBLE EMERGENCIA! 🚨\n\nLLAMA AL 911 INMEDIATAMENTE\n\n(Obteniendo detalles médicos...)"
                    }
                } else {
                    // Si no es grave, mostramos el estado normal
                    runOnUiThread { tvResult.text = "Tú: $textoUsuario\n\nAnalizando gravedad... 🩺" }
                }

                // 3. PENSAR (GEMMA) - Esto ocurre mientras la pantalla YA AVISÓ si era emergencia
                if (cerebroIA != null) {
                    val prompt = "<start_of_turn>user\n" +
                            "Eres un médico de triaje. Clasifica el riesgo en: [BAJO], [MEDIO] o [ALTO] y da un consejo breve.\n" +
                            "Paciente: $textoUsuario<end_of_turn>\n" +
                            "<start_of_turn>model\nRespuesta:"

                    val respuestaIA = cerebroIA!!.generateResponse(prompt)

                    // Pasamos el booleano 'esEmergenciaDetectada' para no volver a calcularlo
                    mostrarResultado(textoUsuario, respuestaIA, esEmergenciaDetectada)

                } else {
                    runOnUiThread { tvResult.text = "Error: Cerebro no disponible" }
                }

            } catch (e: Exception) {
                runOnUiThread { tvResult.text = "Error: ${e.message}" }
            } finally {
                runOnUiThread {
                    btnRecord.isEnabled = true
                    btnRecord.text = "GRABAR (3s)"
                }
            }
        }.start()
    }

    private fun mostrarResultado(usuario: String, respuestaIA: String, esEmergenciaPrevia: Boolean) {
        runOnUiThread {
            val respuestaNorm = respuestaIA.uppercase()

            val colorRojo = 0xFFFF4444.toInt()
            val colorAmarillo = 0xFFFFBB33.toInt()
            val colorVerde = 0xFF99CC00.toInt()

            // Limpieza del texto de la IA
            val consejoIA = respuestaIA
                .replace("[ALTO]", "")
                .replace("[MEDIO]", "")
                .replace("[BAJO]", "")
                .trim()

            // LÓGICA DE VISUALIZACIÓN
            if (esEmergenciaPrevia) {
                // --- MODO EMERGENCIA (Ya estaba en rojo, solo actualizamos el texto abajo) ---
                window.decorView.setBackgroundColor(colorRojo)
                tvResult.text = """
                    🚨 ESTA ES UNA EMERGENCIA, LLAMA AL 911 INMEDIATAMENTE.
                    
                    ----------------
                    Recomendación adicional (IA):
                    $consejoIA
                """.trimIndent()

            } else {
                // --- MODO NORMAL (La IA decide el color) ---
                val colorFondo = when {
                    respuestaNorm.contains("[ALTO]") -> colorRojo
                    respuestaNorm.contains("[MEDIO]") -> colorAmarillo
                    else -> colorVerde
                }
                window.decorView.setBackgroundColor(colorFondo)
                tvResult.text = "Tú: $usuario\n\n🤖 SanaIA: $consejoIA"
            }
        }
    }

    // --- FUNCIONES DE AUDIO Y PERMISOS (Standard) ---
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

    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarGrabacion()
        }
    }

    private fun getModelPath(assetName: String): String {
        val file = File(filesDir, assetName)
        if (!file.exists()) {
            try {
                assets.open("models/$assetName").use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            } catch (e: Exception) { Log.e("SANA", "Error asset: $e") }
        }
        return file.absolutePath
    }
}