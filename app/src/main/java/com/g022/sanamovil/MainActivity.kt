package com.g022.sanamovil

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
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
    private lateinit var btnEnviar: Button
    private lateinit var etSintomas: EditText
    private lateinit var tvResult: TextView
    private lateinit var tvNivel: TextView
    private var isWhisperLoaded = false
    private var cerebroIA: LlmInference? = null

    private val triggersEmergencia = listOf(
        "infarto", "paro", "corazón", "arritmia",
        "asfixia", "ahogo", "no respira", "azul",
        "desmayo", "inconsciente", "convulsion", "derrame", "acv", "despierta",
        "hemorragia", "sangrado", "sangre", "baleado", "disparo", "puñalada", "cuchillo", "quemadura",
        "suicidio", "matarme", "veneno"
    )

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
        btnEnviar = findViewById(R.id.btnEnviar)
        etSintomas = findViewById(R.id.etSintomas)
        tvResult = findViewById(R.id.tvResult)
        tvNivel = findViewById(R.id.tvNivel)

        btnRecord.isEnabled = false
        btnEnviar.isEnabled = false
        tvResult.text = "Iniciando sistemas..."
        tvNivel.visibility = View.GONE

        Thread {
            val whisperPath = getModelPath("ggml-tiny.bin")
            if (File(whisperPath).exists()) {
                isWhisperLoaded = loadModel(whisperPath)
            }

            val modelName = "gemma-2b-it-cpu-int4.bin"
            val modelFile = File(filesDir, modelName)

            if (modelFile.exists()) {
                try {
                    val options = LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(1500)      // <--- CAMBIO 1: Aumentar de 512 a 1500
                        .setMaxTopK(40)
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
                    btnEnviar.isEnabled = true
                    val estado = if (cerebroIA != null) "Sistema listo" else "IA no disponible"
                    tvResult.text = "$estado\n\nPresiona el botón para grabar o escribe tus síntomas"
                } else {
                    tvResult.text = "Error: Whisper no cargó"
                }
            }
        }.start()

        btnRecord.setOnClickListener {
            if (checkPermissions()) iniciarGrabacion()
            else requestPermissions()
        }

        btnEnviar.setOnClickListener {
            val textoIngresado = etSintomas.text.toString().trim()
            if (textoIngresado.isNotEmpty()) {
                procesarTexto(textoIngresado)
                etSintomas.text.clear()
            }
        }
    }

    private fun procesarTexto(textoUsuario: String) {
        btnEnviar.isEnabled = false
        btnRecord.isEnabled = false
        tvNivel.visibility = View.GONE

        runOnUiThread {
            tvResult.text = "Analizando..."
        }

        Thread {
            try {
                Log.d("SANA", "Usuario (texto): $textoUsuario")

                val esEmergenciaDetectada = triggersEmergencia.any {
                    textoUsuario.lowercase().contains(it)
                }

                if (esEmergenciaDetectada) {
                    runOnUiThread {
                        tvNivel.visibility = View.VISIBLE
                        tvNivel.text = "EMERGENCIA"
                        tvNivel.setBackgroundColor(0xFFFF4444.toInt())
                        tvResult.text = "LLAMA AL 911 INMEDIATAMENTE\n\n(Obteniendo detalles médicos...)"
                    }
                } else {
                    runOnUiThread {
                        tvResult.text = "Analizando gravedad... 🩺"
                    }
                }

                if (cerebroIA != null) {
                    val prompt = "<start_of_turn>user\n" +
                            "Actúa como un médico experto y riguroso. Tu objetivo es realizar un triage clínico basado en los síntomas del paciente.\n" +
                            "\n" +
                            "INSTRUCCIONES DE ANÁLISIS:\n" +
                            "1. Evalúa la gravedad basándote en palabras clave de emergencia (dolor de pecho, asfixia, sangrado = Rojo).\n" +
                            "2. Sé específico en las causas (usa terminología médica básica explicada).\n" +
                            "3. Da recomendaciones prácticas y no genéricas.\n" +
                            "4. Analiza EXCLUSIVAMENTE los síntomas que el paciente describe abajo.\n" +
                            "5. NO inventes síntomas que el paciente no mencionó.\n" +
                            "6. NO copies los ejemplos.\n" +
                            "\n" +
                            "Debes responder ESTRICTAMENTE con este formato:\n" +
                            "Nivel: [Leve (Verde) / Moderado (Amarillo) / Severo (Rojo)]\n" +
                            "Posibles causas: [Lista de 4-5 causas probables, de común a rara]\n" +
                            "Recomendaciones: [3 pasos accionables y claros]\n" +
                            "Buscar a un médico si: [Lista específica de signos de alarma para este síntoma]\n" +
                            "\n" +
                            "EJEMPLO 1 (Leve):\n" +
                            "Paciente: \"Me pica mucho la piel del brazo y se puso roja después de tocar una planta.\"\n" +
                            "Respuesta:\n" +
                            "Nivel: Leve (Verde)\n" +
                            "Posibles causas: Dermatitis de contacto, reacción alérgica leve, picadura de insecto, urticaria, irritación por savia.\n" +
                            "Recomendaciones: Lave la zona con agua y jabón neutro inmediatamente, aplique compresas frías para reducir la inflamación y evite rascarse para prevenir infecciones.\n" +
                            "Buscar a un médico si: La erupción se extiende a otras partes del cuerpo, hay hinchazón en la cara o dificultad para respirar.\n" +
                            "\n" +
                            "EJEMPLO 2 (Severo):\n" +
                            "Paciente: \"Siento una presión fuerte en el pecho y me cuesta respirar.\"\n" +
                            "Respuesta:\n" +
                            "Nivel: Severo (Rojo)\n" +
                            "Posibles causas: Infarto agudo de miocardio, angina de pecho, embolia pulmonar, crisis de ansiedad severa, neumotórax.\n" +
                            "Recomendaciones: Siéntese y trate de mantener la calma, afloje la ropa ajustada. NO conduzca al hospital usted mismo.\n" +
                            "Buscar a un médico si: ¡ATENCIÓN INMEDIATA! Llame a emergencias ya si el dolor irradia al brazo izquierdo o mandíbula, o si hay sudoración fría y desmayo.\n" +
                            "\n" +
                            "Paciente: \"$textoUsuario\"<end_of_turn>\n" +
                            "<start_of_turn>model\n" +
                            "Respuesta:"

                    val respuestaIA = cerebroIA!!.generateResponse(prompt)
                    mostrarResultado(textoUsuario, respuestaIA, esEmergenciaDetectada)

                } else {
                    runOnUiThread { tvResult.text = "Error: Cerebro no disponible" }
                }

            } catch (e: Exception) {
                runOnUiThread { tvResult.text = "Error: ${e.message}" }
            } finally {
                runOnUiThread {
                    btnRecord.isEnabled = true
                    btnEnviar.isEnabled = true
                }
            }
        }.start()
    }

    private fun iniciarGrabacion() {
        btnRecord.isEnabled = false
        btnEnviar.isEnabled = false
        btnRecord.text = "Escuchando..."
        tvNivel.visibility = View.GONE

        runOnUiThread {
            tvResult.text = "Grabando... ️"
        }

        Thread {
            try {
                val audioData = grabarAudio(3)
                if (audioData.isEmpty()) return@Thread

                runOnUiThread { tvResult.text = "Transcribiendo... " }

                val textoUsuario = transcribeAudio(audioData)
                Log.d("SANA", "Usuario: $textoUsuario")

                val esEmergenciaDetectada = triggersEmergencia.any {
                    textoUsuario.lowercase().contains(it)
                }

                if (esEmergenciaDetectada) {
                    runOnUiThread {
                        tvNivel.visibility = View.VISIBLE
                        tvNivel.text = "EMERGENCIA"
                        tvNivel.setBackgroundColor(0xFFFF4444.toInt())
                        tvResult.text = "LLAMA AL 911 INMEDIATAMENTE\n\n(Obteniendo detalles médicos...)"
                    }
                } else {
                    runOnUiThread { tvResult.text = "Analizando gravedad... 🩺" }
                }

                if (cerebroIA != null) {
                    val prompt = "<start_of_turn>user\n" +
                            "Actúa como un médico experto y riguroso. Tu objetivo es realizar un triage clínico basado en los síntomas del paciente.\n" +
                            "\n" +
                            "INSTRUCCIONES DE ANÁLISIS:\n" +
                            "1. Evalúa la gravedad basándote en palabras clave de emergencia (dolor de pecho, asfixia, sangrado = Rojo).\n" +
                            "2. Sé específico en las causas (usa terminología médica básica explicada).\n" +
                            "3. Da recomendaciones prácticas y no genéricas.\n" +
                            "4. Analiza EXCLUSIVAMENTE los síntomas que el paciente describe abajo.\n" +
                            "5. NO inventes síntomas que el paciente no mencionó.\n" +
                            "6. NO copies los ejemplos.\n" +
                            "\n" +
                            "Debes responder ESTRICTAMENTE con este formato:\n" +
                            "Nivel: [Leve (Verde) / Moderado (Amarillo) / Severo (Rojo)]\n" +
                            "Posibles causas: [Lista de 4-5 causas probables, de común a rara]\n" +
                            "Recomendaciones: [3 pasos accionables y claros]\n" +
                            "Buscar a un médico si: [Lista específica de signos de alarma para este síntoma]\n" +
                            "\n" +
                            "EJEMPLO 1 (Leve):\n" +
                            "Paciente: \"Me pica mucho la piel del brazo y se puso roja después de tocar una planta.\"\n" +
                            "Respuesta:\n" +
                            "Nivel: Leve (Verde)\n" +
                            "Posibles causas: Dermatitis de contacto, reacción alérgica leve, picadura de insecto, urticaria, irritación por savia.\n" +
                            "Recomendaciones: Lave la zona con agua y jabón neutro inmediatamente, aplique compresas frías para reducir la inflamación y evite rascarse para prevenir infecciones.\n" +
                            "Buscar a un médico si: La erupción se extiende a otras partes del cuerpo, hay hinchazón en la cara o dificultad para respirar.\n" +
                            "\n" +
                            "EJEMPLO 2 (Severo):\n" +
                            "Paciente: \"Siento una presión fuerte en el pecho y me cuesta respirar.\"\n" +
                            "Respuesta:\n" +
                            "Nivel: Severo (Rojo)\n" +
                            "Posibles causas: Infarto agudo de miocardio, angina de pecho, embolia pulmonar, crisis de ansiedad severa, neumotórax.\n" +
                            "Recomendaciones: Siéntese y trate de mantener la calma, afloje la ropa ajustada. NO conduzca al hospital usted mismo.\n" +
                            "Buscar a un médico si: ¡ATENCIÓN INMEDIATA! Llame a emergencias ya si el dolor irradia al brazo izquierdo o mandíbula, o si hay sudoración fría y desmayo.\n" +
                            "\n" +
                            "Paciente: \"$textoUsuario\"<end_of_turn>\n" +
                            "<start_of_turn>model\n" +
                            "Respuesta:"

                    val respuestaIA = cerebroIA!!.generateResponse(prompt)
                    mostrarResultado(textoUsuario, respuestaIA, esEmergenciaDetectada)

                } else {
                    runOnUiThread { tvResult.text = "Error: Cerebro no disponible" }
                }

            } catch (e: Exception) {
                runOnUiThread { tvResult.text = "Error: ${e.message}" }
            } finally {
                runOnUiThread {
                    btnRecord.isEnabled = true
                    btnEnviar.isEnabled = true
                    btnRecord.text = "Presiona para hablar (3S)"
                }
            }
        }.start()
    }

    private fun mostrarResultado(usuario: String, respuestaIA: String, esEmergenciaPrevia: Boolean) {
        runOnUiThread {
            val respuestaNorm = respuestaIA.uppercase()

            val colorRojo = 0xFFFF4444.toInt()
            val colorAmarillo = 0xFFFFBB33.toInt()
            val colorVerde = 0xFF4CAF50.toInt()

            if (esEmergenciaPrevia) {
                tvNivel.visibility = View.VISIBLE
                tvNivel.text = "EMERGENCIA"
                tvNivel.setBackgroundColor(colorRojo)
                tvResult.text = """
                    LLAMA AL 911 INMEDIATAMENTE
                    
                    Análisis clínico (IA):
                    $respuestaIA
                """.trimIndent()

            } else {
                val (colorFondo, nivelTexto) = when {
                    respuestaNorm.contains("(ROJO)") || respuestaNorm.contains("SEVERO") ->
                        Pair(colorRojo, "Severo")
                    respuestaNorm.contains("(AMARILLO)") || respuestaNorm.contains("MODERADO") ->
                        Pair(colorAmarillo, "Moderado")
                    respuestaNorm.contains("(VERDE)") || respuestaNorm.contains("LEVE") ->
                        Pair(colorVerde, "Leve")
                    else -> Pair(colorVerde, "Leve")
                }

                tvNivel.visibility = View.VISIBLE
                tvNivel.text = nivelTexto
                tvNivel.setBackgroundColor(colorFondo)

                val textoLimpio = respuestaIA.replace("Respuesta:", "").trim()
                tvResult.text = textoLimpio
            }
        }
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
}