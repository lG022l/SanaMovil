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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    // Referencias a la UI
    private lateinit var btnRecord: Button
    private lateinit var tvResult: TextView

    // Estado del modelo
    private var isModelLoaded = false

    // Funciones nativas (C++)
    external fun loadModel(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray): String

    companion object {
        init { System.loadLibrary("sanamovil") } // Recuerda: el nombre debe coincidir con CMakeLists
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular UI
        btnRecord = findViewById(R.id.btnRecord)
        tvResult = findViewById(R.id.tvResult)

        // Cargar modelo al iniciar en segundo plano
        btnRecord.isEnabled = false // Desactivar botón hasta que cargue
        tvResult.text = "Cargando modelo Whisper..."

        Thread {
            val modelPath = getModelPath("ggml-tiny.bin")
            if (File(modelPath).exists()) {
                isModelLoaded = loadModel(modelPath)
                runOnUiThread {
                    if (isModelLoaded) {
                        btnRecord.isEnabled = true
                        tvResult.text = "Modelo listo. \nPresiona Grabar."
                    } else {
                        tvResult.text = "Error: No se pudo cargar el modelo."
                    }
                }
            } else {
                runOnUiThread { tvResult.text = "Error: Archivo de modelo no encontrado." }
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
        // Deshabilitar botón para evitar doble click
        btnRecord.isEnabled = false
        btnRecord.text = "Grabando..."
        tvResult.text = "Escuchando..."

        Thread {
            try {
                // Grabar 3 segundos
                val audioData = grabarAudio(3)

                if (audioData.isNotEmpty()) {
                    runOnUiThread { tvResult.text = "Procesando voz..." }

                    // Transcribir con Whisper
                    val textoTranscrito = transcribeAudio(audioData)

                    // Mostrar trancripción en pantalla
                    runOnUiThread {
                        tvResult.text = textoTranscrito
                        Log.d("WHISPER_RESULT", textoTranscrito)
                    }
                } else {
                    runOnUiThread { tvResult.text = "Error: No se grabó audio." }
                }
            } catch (e: Exception) {
                runOnUiThread { tvResult.text = "Error: ${e.message}" }
            } finally {
                // Reactivar botón
                runOnUiThread {
                    btnRecord.isEnabled = true
                    btnRecord.text = "GRABAR (3s)"
                }
            }
        }.start()
    }

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

        // Convertir a Float para Whisper
        return FloatArray(audioDataShort.size) { i ->
            audioDataShort[i] / 32768.0f
        }
    }

    // --- Gestión de Permisos y Archivos  ---
    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarGrabacion() // Iniciar automáticamente si da permiso
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
            } catch (e: Exception) { Log.e("SANA", "Error copiando: $e") }
        }
        return file.absolutePath
    }
}