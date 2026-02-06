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
// 1. IMPORTACIONES DE MEDIAPIPE (EL NUEVO CEREBRO)
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions

class MainActivity : AppCompatActivity() {

    // Referencias a la UI
    private lateinit var btnRecord: Button
    private lateinit var tvResult: TextView

    // Estado de los modelos
    private var isWhisperLoaded = false
    private var cerebroIA: LlmInference? = null // Variable para el cerebro de Google

    // --- FUNCIONES NATIVAS (SOLO WHISPER) ---
    // Ya borramos todo lo de Llama. Solo queda el oído en C++.
    external fun loadModel(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray): String

    companion object {
        init { System.loadLibrary("sanamovil") }
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular UI
        btnRecord = findViewById(R.id.btnRecord)
        tvResult = findViewById(R.id.tvResult)

        btnRecord.isEnabled = false
        tvResult.text = "Iniciando sistemas..."

        // Cargar modelos en segundo plano
        Thread {
            // A. CARGAR WHISPER (C++ Nativo)
            val whisperPath = getModelPath("ggml-tiny.bin")
            if (File(whisperPath).exists()) {
                isWhisperLoaded = loadModel(whisperPath)
            }

            // B. CARGAR CEREBRO (MediaPipe - Google)
            // IMPORTANTE: MediaPipe NO usa .gguf, usa archivos .bin
            val modelName = "gemma-2b-it-cpu-int4.bin"
            val modelFile = File(filesDir, modelName)

            if (modelFile.exists()) {
                try {
                    val options = LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(1000) // Respuesta más larga
                        .setTopK(40)
                        .setTemperature(0.7f) // Creatividad balanceada
                        .build()

                    cerebroIA = LlmInference.createFromOptions(this, options)
                    Log.d("SANA", "MediaPipe cargado exitosamente 🚀")
                } catch (e: Exception) {
                    Log.e("SANA", "Error cargando MediaPipe: ${e.message}")
                }
            } else {
                Log.e("SANA", "Falta el archivo $modelName")
            }

            // Actualizar Pantalla
            runOnUiThread {
                if (isWhisperLoaded) {
                    btnRecord.isEnabled = true
                    val estadoCerebro = if (cerebroIA != null) "Cerebro ACTIVO 🧠 (CPU)" else "Cerebro DESCONECTADO (Falta .bin)"
                    tvResult.text = "Whisper Listo 👂.\n$estadoCerebro\n\nPresiona Grabar."
                } else {
                    tvResult.text = "Error: Whisper no pudo cargar."
                }
            }
        }.start()

        // Botón
        btnRecord.setOnClickListener {
            if (checkPermissions()) {
                iniciarGrabacion()
            } else {
                requestPermissions()
            }
        }
    }

    private fun iniciarGrabacion() {
        btnRecord.isEnabled = false
        btnRecord.text = "Escuchando..."
        tvResult.text = "Grabando audio..."

        Thread {
            try {
                // 1. GRABAR
                val audioData = grabarAudio(3)

                if (audioData.isNotEmpty()) {
                    runOnUiThread { tvResult.text = "Transcribiendo..." }

                    // 2. TRANSCRIBIR (Whisper C++)
                    val textoUsuario = transcribeAudio(audioData)
                    Log.d("SANA", "Usuario: $textoUsuario")

                    runOnUiThread {
                        tvResult.text = "Tú: $textoUsuario\n\nGenerando diagnóstico..."
                    }

                    // 3. PENSAR (MediaPipe)
                    if (cerebroIA != null) {
                        try {
                            // ¡Aquí ocurre la magia de Google!
                            val respuesta = cerebroIA!!.generateResponse(textoUsuario)

                            runOnUiThread {
                                tvResult.text = "Tú: $textoUsuario\n\n🤖 SanaIA: $respuesta"
                            }
                        } catch (e: Exception) {
                            runOnUiThread { tvResult.text = "Error al pensar: ${e.message}" }
                        }
                    } else {
                        runOnUiThread {
                            tvResult.text = "Tú: $textoUsuario\n\n(Cerebro desconectado. Sube un archivo .bin)"
                        }
                    }

                } else {
                    runOnUiThread { tvResult.text = "No te escuché bien." }
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

    // --- Gestión de Audio y Permisos (Igual que siempre) ---
    private fun grabarAudio(durationSecs: Int): FloatArray {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return FloatArray(0)
        }

        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return FloatArray(0)

        val audioDataShort = ShortArray(sampleRate * durationSecs)
        recorder.startRecording()
        recorder.read(audioDataShort, 0, audioDataShort.size)
        recorder.stop()
        recorder.release()

        return FloatArray(audioDataShort.size) { i -> audioDataShort[i] / 32768.0f }
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
                assets.open("models/$assetName").use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) { Log.e("SANA", "Error asset: $e") }
        }
        return file.absolutePath
    }

    override fun onDestroy() {
        super.onDestroy()
        // No hay nada nativo que limpiar de Llama, MediaPipe se limpia solo o con close() si quisieras
    }
}