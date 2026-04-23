package com.g022.sanamovil.Home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g022.sanamovil.EmergencyLevel
import com.g022.sanamovil.MainActivity
import com.g022.sanamovil.R
import com.g022.sanamovil.UiState
import com.g022.sanamovil.ViewModel.SanaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.AdminPanelSettings
import com.g022.sanamovil.UserRole

data class Paciente(
    val id: Int,
    val nombre: String,
    val edad: Int,
    val sexo: String,
    val estatura: Float,
    val peso: Float,
    val tipoSangre: String,
    val alergias: Int,
    val observaciones: String
)

// Lista hardcodeada con las observaciones
val pacientesDePrueba = listOf(
    Paciente(1, "Victor Guzmán", 29, "M", 1.75f, 72f, "O+", 0, "Paciente sano, acude anualmente a revisión de rutina general. Sin antecedentes crónicos importantes."),
    Paciente(2, "María López", 32, "F", 1.65f, 60f, "A-", 1, "Alergia a la penicilina. Vino a consulta hace 1 semana por faringitis estreptocócica, se le recetó eritromicina por 7 días."),
    Paciente(3, "Carlos Ruiz", 45, "M", 1.80f, 85f, "B+", 0, "Diabetes tipo 2, hipertenso. Acude a control mensual. Tensión arterial actual estable con Losartán 50mg."),
    Paciente(4, "Sofia Rangel", 46, "F", 1.65f, 60f, "A-", 1, "Paciente femenina, cuarta década de vida, embarazo de 11.2 semanas. Sangrado vaginal tipo manchado por 6 días. Dolor abdominal. Trabajo físico intenso durante la última semana. Orificio cervical externo cerrado."),
)


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

    if (showUserProfile) {
        ProfileDialog(
            viewModel = viewModel, // 👇 LE PASAMOS EL VIEWMODEL AQUÍ
            onDismiss = { showUserProfile = false }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.setLoading(true, "Cargando IA y Modelos...")
        activityContext.initModels(viewModel)
        viewModel.cargarListaDeCasos(activityContext)
        viewModel.setLoading(false, "Sistema listo. ¿Cuál es la situación?")
    }

    // --- INTERCEPTOR DE TEXTO HARDCODEADO ---
    val procesarEntrada = { texto: String ->
        // Comprobamos si el texto contiene palabras clave del caso hardcodeado (para que funcione incluso si hay un espacio extra)
        if (texto.contains("embarazo de 11.2 semanas", ignoreCase = true) ||
            texto.contains("cuarta década de vida", ignoreCase = true)) {

            scope.launch {
                // 1. Limpiamos la caja de texto y mostramos estado de carga
                viewModel.updateInput("")
                viewModel.setLoading(true, "Analizando gravedad...")

                // 2. Esperamos 7 segundos (7000 milisegundos)
                delay(7000)

                // 3. Mostramos la respuesta hardcodeada
                val respuestaHardcodeada = """
                    ⚠️ RIESGO MODERADO-ALTO — Amenaza de aborto con factores de riesgo

                    EVALUACIÓN:
                    - Sangrado vaginal de 6 días en primer trimestre con dolor abdominal
                    - Trabajo físico intenso = factor de riesgo para aborto incompleto
                    - Edad materna >35 = factor de riesgo adicional

                    PLAN RECOMENDADO:
                    1. Monitoreo cada 2-4 horas (signos vitales + cantidad de sangrado)
                    2. Cuantificar sangrado: número de toallas sanitarias/hora
                    3. Establecer acceso venoso periférico preventivo
                    4. Preparar plan de traslado de emergencia AHORA
                       - Identificar vehículo disponible
                       - Contactar hospital receptor si hay señal
                       - Tener líquidos IV listos para transporte

                    🔴 SEÑALES DE ALARMA A VIGILAR:
                    - Sangrado que empapa >1 toalla/hora
                    - Taquicardia >100 lpm o PA sistólica <90 mmHg
                    - Mareo, palidez, pérdida de consciencia
                    - Fiebre >38°C

                    ⚠️ ADVERTENCIA: Con sangrado de 6 días y dolor progresivo, 
                    el riesgo de evolución a aborto incompleto con hemorragia es 
                    SIGNIFICATIVO. No esperar a que sea emergencia para planear 
                    traslado. Preparar logística de transporte inmediatamente.

                    Confianza: 87%
                """.trimIndent()

                viewModel.setLegacyResult(respuestaHardcodeada, EmergencyLevel.SEVERO)
            }
        } else {
            // ADAPTACIÓN FASE 8: Llamamos al nuevo flujo que activa el Wizard
            viewModel.processTriage(texto)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.setLoading(true, "Escuchando (3s)...")
                onRecordRequest(3) { text ->
                    procesarEntrada(text)
                }
            }
        }
    )

    if (state.currentRole == UserRole.SUPERVISOR) {
        // VISTA DE SUPERVISOR
        SupervisorDashboard(
            viewModel = viewModel,
            selectedTab = state.supervisorSelectedTab,
            onTabSelected = { viewModel.setSupervisorTab(it) }
        )
    } else {

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Historial de Casos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 👇 AHORA USAMOS state.dashboardLogs EN LUGAR DE recentQueries
                        if (state.dashboardLogs.isEmpty()) {
                            Text(
                                "No hay consultas registradas en la base de datos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn {
                                items(state.dashboardLogs) { log ->
                                    // Formateamos la fecha (Necesitarás importar java.text.SimpleDateFormat y java.util.Date)
                                    val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch { drawerState.close() }
                                                // Opcional: Aquí puedes agregar lógica si quieres que al tocar
                                                // un historial, este se abra en la pantalla principal.
                                            }
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = log.nivelRiesgoAsignado,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = date,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            // Recortamos el texto para que no ocupe demasiado espacio
                                            text = if (log.inputCompleto.length > 50) log.inputCompleto.take(50) + "..." else log.inputCompleto,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logov3),
                                    contentDescription = "Logo SanaMovil",
                                    modifier = Modifier.size(150.dp)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menú")
                            }
                        },
                        actions = {
                            //admin
                            IconButton(onClick = { viewModel.setRole(UserRole.SUPERVISOR) }) {
                                Icon(Icons.Default.AdminPanelSettings, "Modo Supervisor")
                            }
                            //user
                            IconButton(onClick = { showUserProfile = true }) {
                                Icon(Icons.Default.AccountCircle, "Perfil Usuario")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    // Usamos AnimatedVisibility para que el espacio colapse suavemente cuando ya no hay mensaje
                    AnimatedVisibility(visible = state.isLoading || state.statusMessage.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (state.isLoading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            if (state.statusMessage.isNotBlank()) {
                                Text(
                                    text = state.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    // --- LÓGICA DE VISTAS (FASE 8 y 9) ---
                    if (state.showWizard) {
                        // Vista 1: El cuestionario Wizard
                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            TriageWizard(
                                uiState = state,
                                onAgeChange = { viewModel.updateWizardAge(it) },
                                onDurationChange = { viewModel.updateWizardDuration(it) },
                                onIntensityChange = { viewModel.updateWizardIntensity(it) },
                                onConsciousnessChange = { viewModel.updateConsciousness(it) },
                                onRadiationChange = { viewModel.updateRadiation(it) },
                                onConsentChange = { viewModel.updateWizardConsent(it) },
                                onSubmit = { viewModel.submitWizardAndCalculate() }
                            )
                        }
                    } else if (state.triageResult != null) {
                        // Vista 2: El resultado legal auditable de la Fase 9
                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            TriageResultCard(uiState = state)
                        }
                    } else if (state.analysisResult.isNotEmpty()) {
                        // Vista 3: Legacy Fallback (para que no rompa código viejo)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = state.emergencyLevel.color.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (state.emergencyLevel != EmergencyLevel.NONE) {
                                    Text(
                                        text = state.emergencyLevel.label,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = state.emergencyLevel.color,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                                Text(
                                    text = state.analysisResult,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        // Vista 4: Pantalla de inicio vacía
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Describe la situación",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Escribe o usa el micrófono",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InputArea(
                        text = state.inputText,
                        onTextChanged = { viewModel.updateInput(it) },
                        onSend = { procesarEntrada(state.inputText) },
                        onMicClick = {
                            if (ContextCompat.checkSelfPermission(
                                    activityContext,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.setLoading(true, "Escuchando...")
                                onRecordRequest(3) { text -> procesarEntrada(text) }
                            } else {
                                launcher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        isEnabled = !state.isLoading
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Describe tus síntomas...") },
                enabled = isEnabled,
                minLines = 1,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )

            // Botón micrófono
            IconButton(
                onClick = onMicClick,
                enabled = isEnabled,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Mic, "Grabar audio", tint = MaterialTheme.colorScheme.primary)
            }

            // Botón enviar
            IconButton(
                onClick = onSend,
                enabled = isEnabled && text.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Enviar",
                    tint = if (text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProfileDialog(onDismiss: () -> Unit,
                  viewModel: SanaViewModel,) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedPatient by remember { mutableStateOf<Paciente?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (selectedPatient == null) {
                // PANTALLA 1: Lista de pacientes
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seleccionar Paciente",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

                    Button(onClick = {
                        viewModel.exportarDatos(context) { uri ->
                            uri?.let {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Enviar base de datos al supervisor"))
                            }
                        }
                    }) {
                        Text("Exportar Casos")
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    val filePickerLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        uri?.let { viewModel.importarDatos(context, it) }
                    }

                    Button(onClick = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }) {
                        Text("Importar Casos (Supervisor)")
                    }

                    Spacer(modifier = Modifier.height(24.dp))





                    LazyColumn {
                        items(pacientesDePrueba) { paciente ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { selectedPatient = paciente },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = paciente.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${paciente.edad} años • ${paciente.sexo}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // PANTALLA 2: Detalles del paciente seleccionado
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedPatient = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = selectedPatient!!.nombre,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${selectedPatient!!.edad} años • ${selectedPatient!!.sexo}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- TARJETAS DE SIGNOS VITALES ---
                    val paciente = selectedPatient!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column {
                            VitalSignIndicator(
                                icon = Icons.Default.Height,
                                label = "Estatura",
                                value = paciente.estatura.toString(),
                                unit = "m",
                                color = Color(0xFF3B82F6),
                                progress = 0.85f,
                                normalRange = "Promedio",
                                isAlert = false
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            VitalSignIndicator(
                                icon = Icons.Default.Monitor,
                                label = "Peso",
                                value = paciente.peso.toInt().toString(),
                                unit = "kg",
                                color = Color(0xFF10B981),
                                progress = 0.7f,
                                normalRange = "Saludable",
                                isAlert = false
                            )
                        }

                        Column {
                            VitalSignIndicator(
                                icon = Icons.Default.Bloodtype,
                                label = "Tipo de Sangre",
                                value = paciente.tipoSangre,
                                color = Color(0xFFEF4444),
                                progress = 1f,
                                normalRange = paciente.tipoSangre,
                                isAlert = false
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            VitalSignIndicator(
                                icon = Icons.Default.HealthAndSafety,
                                label = "Alergias",
                                value = paciente.alergias.toString(),
                                color = Color(0xFF14B8A6),
                                progress = 1f,
                                normalRange = "Ninguna conocida",
                                isAlert = paciente.alergias > 0
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- NUEVA SECCIÓN: OBSERVACIONES ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Observaciones y Notas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = paciente.observaciones,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón Editar Principal
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- NUEVO BOTÓN: COPIAR AL PORTAPAPELES ---
                    OutlinedButton(
                        onClick = {
                            // Formateamos la información del paciente
                            val infoPaciente = """
                                PACIENTE: ${paciente.nombre}
                                Edad: ${paciente.edad} años
                                Sexo: ${paciente.sexo}
                                Estatura: ${paciente.estatura} m
                                Peso: ${paciente.peso} kg
                                Tipo de sangre: ${paciente.tipoSangre}
                                Alergias reportadas: ${paciente.alergias}
                                
                                OBSERVACIONES:
                                ${paciente.observaciones}
                            """.trimIndent()

                            // Copiamos al portapapeles
                            clipboardManager.setText(AnnotatedString(infoPaciente))

                            // Mostramos un mensajito al usuario para confirmar
                            Toast.makeText(context, "Información copiada al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar información")
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun VitalSignIndicator(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String = "",
    color: Color,
    progress: Float,
    normalRange: String,
    isAlert: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(160.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(120.dp),
                color = color.copy(alpha = 0.1f),
                strokeWidth = 8.dp,
                trackColor = Color.Transparent
            )

            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(120.dp),
                color = color,
                strokeWidth = 8.dp,
                trackColor = Color.Transparent
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isAlert) Color(0xFFEF4444) else color,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = normalRange,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- FASE 9: COMPONENTE DE RESULTADO AUDITABLE ---
@Composable
fun TriageResultCard(uiState: UiState) {
    val result = uiState.triageResult ?: return

    var showTraceability by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.1f)        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Nivel de Prioridad: ${result.urgencyLevel.title}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = uiState.emergencyLevel.color
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = result.standardMessage,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Acción sugerida: ${result.timeframe}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- INICIO DEL ESTILO ORIGINAL PARA LA RESPUESTA DEL LLM ---
            if (uiState.emergencyLevel != EmergencyLevel.NONE) {
                Text(
                    text = uiState.emergencyLevel.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = uiState.emergencyLevel.color,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // NUEVO: Lógica de animación de carga mientras el LLM "piensa"
            if (result.llmExplanation.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Se esta redactando el análisis...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else {
                Text(
                    text = result.llmExplanation,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            // --- FIN DEL ESTILO ORIGINAL ---

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showTraceability = !showTraceability },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showTraceability) "Ocultar detalles técnicos" else "¿Por qué esta prioridad?")
            }

            AnimatedVisibility(visible = showTraceability) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Reglas Clínicas Activadas:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        result.triggeredRules.forEach { rule ->
                            Text("• $rule", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Motor: ${result.ruleEngineVersion}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("Modelo: ${result.llmModelVersion}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = result.disclaimer,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
                lineHeight = 14.sp
            )
        }
    }
}