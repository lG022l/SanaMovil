package com.g022.sanamovil

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit

// --- VIEWMODEL: Maneja el estado de la UI y la lógica de negocio ---
class SanaViewModel : ViewModel() {
    var uiState by mutableStateOf(UiState())
        private set

    var cerebroIA: LlmInference? = null
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
        "suicidio", "matarme", "veneno"
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SanaAppTheme {
                SanaAppScreen(
                    onRecordRequest = { duration, callback -> grabarYProcesarAudio(duration, callback) },
                    onAnalyzeRequest = { text, viewModel -> procesarTexto(text, viewModel) },
                    activityContext = this
                )
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

    }
}

// --- COMPOSABLES (UI) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanaAppScreen(
    onRecordRequest: (Int, (String) -> Unit) -> Unit,
    onAnalyzeRequest: (String, SanaViewModel) -> Unit,
    activityContext: MainActivity
) {
    val viewModel: SanaViewModel = viewModel()
    val state = viewModel.uiState
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showUserProfile by remember { mutableStateOf(false) }

    // ---> AQUÍ ESTÁ EL CAMBIO PRINCIPAL PARA PANTALLA COMPLETA <---
    if (showUserProfile) {
        Dialog(
            onDismissRequest = { showUserProfile = false },
            properties = DialogProperties(usePlatformDefaultWidth = false) // Esto hace que ocupe TODA la pantalla
        ) {
            UserProfileScreen(onDismiss = { showUserProfile = false })
        }
    }

    // Inicializar modelos al arrancar
    LaunchedEffect(Unit) {
        viewModel.setLoading(true, "Cargando IA y Modelos...")
        activityContext.initModels(viewModel)
        viewModel.setLoading(false, "Sistema listo. ¿Cómo te sientes?")
    }

    // Permisos de Audio
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.setLoading(true, "Escuchando (3s)...")
                onRecordRequest(3) { text ->
                    onAnalyzeRequest(text, viewModel)
                }
            }
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Consultas Recientes", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                Divider()
                LazyColumn {
                    items(viewModel.recentQueries) { query ->
                        NavigationDrawerItem(
                            label = { Text(query) },
                            selected = false,
                            onClick = { /* Cargar esa consulta */ },
                            icon = { Icon(Icons.Default.History, null) }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "SanaMovil",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showUserProfile = true }) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil Usuario")
                        }
                    }
                )
            },
            bottomBar = {
                InputArea(
                    text = state.inputText,
                    onTextChanged = { viewModel.updateInput(it) },
                    onSend = { onAnalyzeRequest(state.inputText, viewModel) },
                    onMicClick = {
                        if (ContextCompat.checkSelfPermission(activityContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.setLoading(true, "Escuchando...")
                            onRecordRequest(3) { text -> onAnalyzeRequest(text, viewModel) }
                        } else {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    isEnabled = !state.isLoading
                )
            }
        ) { innerPadding ->
            MainContent(
                state = state,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun MainContent(state: UiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(state.statusMessage)
                }
            }
        } else {
            if (state.analysisResult.isNotEmpty()) {
                // Tarjeta de Resultado
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Ocupa el espacio disponible
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        // Header de Nivel
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(state.emergencyLevel.color, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.emergencyLevel.label.ifEmpty { "RESULTADO" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Contenido Scrollable
                        LazyColumn {
                            item {
                                Text(
                                    text = state.analysisResult,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Estado vacío (Placeholder)
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun InputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isEnabled: Boolean
) {
    Column (){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding(), // Ajuste para el teclado
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Escribe tus síntomas...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                enabled = isEnabled,
                maxLines = 3
            )

            Spacer(Modifier.width(8.dp))

            if (text.isEmpty()) {
                FloatingActionButton(
                    onClick = onMicClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Grabar")
                }
            } else {
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(onDismiss: () -> Unit) {
    // Usamos Surface para que tome el color de fondo del tema (oscuro/claro)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        )
        {

            // 1. Barra Superior con botón "Cerrar"
            CenterAlignedTopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // 2. Contenido del Perfil
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Avatar Grande
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Invitado",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )


                Spacer(modifier = Modifier.height(32.dp))

                // Tarjeta de Datos Médicos
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileItem(label = "Género", value = "Masculino")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileItem(label = "Edad", value = "24 años")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileItem(label = "Tipo Sanguíneo", value = "O+")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileItem(label = "Peso", value = "72 kg")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileItem(label = "Estatura", value = "1.75 m")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileItem(label = "Alergias", value = "Ninguna conocida", isAlert = false)
                    }
                }

                Spacer(modifier = Modifier.weight(1f)) // Empuja el botón al final

                // Botón Editar (Sin funcionalidad real)
                Button(
                    onClick = { /* TODO: Implementar edición */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Información")
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// Pequeño componente auxiliar para las filas de datos
@Composable
fun ProfileItem(label: String, value: String, isAlert: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

// --- TEMA SIMPLE ---
@Composable
fun SanaAppTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xF790CAF9),
            secondary = Color(0xFFCE93D8),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1976D2),
            secondary = Color(0xFF7B1FA2),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}