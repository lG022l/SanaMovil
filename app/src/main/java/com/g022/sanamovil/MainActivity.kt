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
import android.widget.Toast // Agregado para notificaciones breves
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    // Referencias a la UI
    private lateinit var btnRecord: Button
    private lateinit var tvResult: TextView

    // Estado de los modelos
    private var isWhisperLoaded = false
    private var isLlamaLoaded = false // NUEVO: Estado del cerebro

    // --- FUNCIONES NATIVAS (C++) ---
    // 1. Whisper (Oído)
    external fun loadModel(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray): String

    // 2. Llama (Cerebro) - NUEVAS
    external fun loadMedGemma(modelPath: String): Int
    external fun answerPrompt(prompt: String): String
    external fun unloadModel() // Para limpiar memoria al salir

    companion object {
        init { System.loadLibrary("sanamovil") }
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //codigo de prueba para problema con medgemma
        Log.e("SANA_DEBUG", "--- INICIO DE RASTREO DE ARCHIVOS ---")
        Log.e("SANA_DEBUG", "Buscando en la carpeta: ${filesDir.absolutePath}")
        val listaArchivos = filesDir.listFiles()
        if (listaArchivos != null && listaArchivos.isNotEmpty()) {
            for (archivo in listaArchivos) {
                Log.e("SANA_DEBUG", "ENCONTRADO: ${archivo.name} (Tamaño: ${archivo.length()} bytes)")
            }
        } else {
            Log.e("SANA_DEBUG", "¡La carpeta 'files' está VACÍA!")
        }
        Log.e("SANA_DEBUG", "--- FIN DE RASTREO ---")
        // Vincular UI
        btnRecord = findViewById(R.id.btnRecord)
        tvResult = findViewById(R.id.tvResult)

        btnRecord.isEnabled = false
        tvResult.text = "Iniciando sistemas de IA..."

        // Cargar modelos en un hilo secundario para no trabar la app
        Thread {
            // A. CARGAR WHISPER (Como antes)
            val whisperPath = getModelPath("ggml-tiny.bin") // Este sí está en assets
            if (File(whisperPath).exists()) {
                isWhisperLoaded = loadModel(whisperPath)
            }

            // B. CARGAR MEDGEMMA (NUEVO)
            // OJO: Este archivo NO está en assets todavía, lo meteremos manual (Sideload)
            // Buscamos en la carpeta de archivos de la app directamente
            val llamaPath = File(filesDir, "phi-2.Q4_K_M.gguf").absolutePath

            if (File(llamaPath).exists()) {
                val status = loadMedGemma(llamaPath)
                isLlamaLoaded = (status == 0) // 0 significa éxito en nuestro C++
            } else {
                Log.e("SANA", "No se encontró medgemma-2b.gguf en: $llamaPath")
            }

            // Actualizar UI al terminar
            runOnUiThread {
                if (isWhisperLoaded) {
                    btnRecord.isEnabled = true
                    val estadoCerebro = if (isLlamaLoaded) "Cerebro ACTIVO 🧠" else "Cerebro DESCONECTADO (Falta archivo)"
                    tvResult.text = "Whisper Listo 👂.\n$estadoCerebro\n\nPresiona Grabar."
                } else {
                    tvResult.text = "Error crítico: Whisper no cargó."
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
                val audioData = grabarAudio(3) // 3 segundos

                if (audioData.isNotEmpty()) {
                    runOnUiThread { tvResult.text = "Transcribiendo..." }

                    // 2. TRANSCRIBIR (Oído)
                    val textoUsuario = transcribeAudio(audioData)
                    Log.d("SANA", "Usuario dijo: $textoUsuario")

                    // Mostrar lo que entendió
                    runOnUiThread {
                        tvResult.text = "Tú: $textoUsuario\n\nPensando respuesta..."
                    }

                    // 3. PENSAR (Cerebro) - NUEVO
                    if (isLlamaLoaded) {
                        // Enviamos el texto al modelo médico
                        val respuestaMedica = answerPrompt(textoUsuario)

                        // 4. MOSTRAR RESPUESTA FINAL
                        runOnUiThread {
                            tvResult.text = "Tú: $textoUsuario\n\n🤖 SanaIA: $respuestaMedica"
                        }
                    } else {
                        runOnUiThread {
                            tvResult.text = "Tú: $textoUsuario\n\n(El cerebro médico no está cargado. Sube el archivo .gguf)"
                        }
                    }

                } else {
                    runOnUiThread { tvResult.text = "Error: No se escuchó nada." }
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

    // --- (El resto de funciones siguen IGUAL que antes) ---

    private fun grabarAudio(durationSecs: Int): FloatArray {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return FloatArray(0)
        }

        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("SANA", "Error iniciando AudioRecord")
            return FloatArray(0)
        }

        val audioDataShort = ShortArray(sampleRate * durationSecs)

        recorder.startRecording()
        recorder.read(audioDataShort, 0, audioDataShort.size)
        recorder.stop()
        recorder.release()

        return FloatArray(audioDataShort.size) { i ->
            audioDataShort[i] / 32768.0f
        }
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
        // Solo copiamos si no existe, para ahorrar tiempo
        if (!file.exists()) {
            try {
                assets.open("models/$assetName").use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) { Log.e("SANA", "Error copiando asset $assetName: $e") }
        }
        return file.absolutePath
    }

    // Limpieza al cerrar la app
    override fun onDestroy() {
        super.onDestroy()
        unloadModel() // Liberar RAM
    }
}