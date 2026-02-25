package com.g022.sanamovil.Home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g022.sanamovil.MainActivity
import com.g022.sanamovil.UiState
import kotlinx.coroutines.launch
import kotlin.text.ifEmpty
import com.g022.sanamovil.ViewModel.SanaViewModel
import com.g022.sanamovil.EmergencyLevel
import androidx.compose.foundation.lazy.items

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
                .padding(16.dp),
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

            // 2. Contenido del Perfil (SCROLLEABLE)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                        // Primera fila
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            VitalSignIndicator(
                                icon = Icons.Default.Person,
                                label = "Género",
                                value = "M",
                                color = Color(0xFF818181),
                                progress = 1f,
                                normalRange = "Masculino"
                            )

                            VitalSignIndicator(
                                icon = Icons.Default.Cake,
                                label = "Edad",
                                value = "24",
                                unit = "años",
                                color = Color(0xFFEC4899),
                                progress = 1f,
                                normalRange = "---"
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Segunda fila
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            VitalSignIndicator(
                                icon = Icons.Default.Height,
                                label = "Estatura",
                                value = "1.75",
                                unit = "m",
                                color = Color(0xFF3B82F6),
                                progress = 1f,
                                normalRange = "---"
                            )


                            VitalSignIndicator(
                                icon = Icons.Default.Monitor,
                                label = "Peso",
                                value = "72",
                                unit = "kg",
                                color = Color(0xFF10B981),
                                progress = 0.72f,
                                normalRange = "60-90 kg"
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tercera fila
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            VitalSignIndicator(
                                icon = Icons.Default.Bloodtype,
                                label = "Tipo Sanguíneo",
                                value = "O+",
                                color = Color(0xFFEF4444),
                                progress = 1f,
                                normalRange = "A, B, AB, O"
                            )

                            VitalSignIndicator(
                                icon = Icons.Default.HealthAndSafety,
                                label = "Alergias",
                                value = "0",
                                color = Color(0xFF14B8A6),
                                progress = 1f,
                                normalRange = "Ninguna conocida",
                                isAlert = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
        // Ícono y etiqueta
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

        // Indicador circular
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // Círculo de fondo
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.size(120.dp),
                color = color.copy(alpha = 0.1f),
                strokeWidth = 8.dp,
                trackColor = Color.Transparent
            )

            // Círculo de progreso
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(120.dp),
                color = color,
                strokeWidth = 8.dp,
                trackColor = Color.Transparent
            )

            // Valor central
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

        // Rango normal
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

// --- TEMA SIMPLE ---
@Composable
fun SanaAppTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF47FA4A),
            secondary = Color(0xFF22B424),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF22B424),
            secondary = Color(0xFF188F1A),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}