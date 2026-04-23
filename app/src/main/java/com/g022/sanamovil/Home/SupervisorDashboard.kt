package com.g022.sanamovil.Home

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.g022.sanamovil.UserRole
import com.g022.sanamovil.ViewModel.SanaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.window.Dialog
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboard(
    viewModel: SanaViewModel,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("General", "Casos", "Métricas", "Alertas", "Config")
    val icons = listOf(
        Icons.Default.Dashboard,
        Icons.Default.ListAlt,
        Icons.Default.Analytics,
        Icons.Default.Warning,
        Icons.Default.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Supervisor", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Botón para salir del modo supervisor (volver a brigadista)
                    IconButton(onClick = { viewModel.setRole(UserRole.OPERATOR) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Salir de Supervisor")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> VistaGeneralSkeleton(viewModel) // <-- Aquí faltaba el viewModel
                1 -> CasosScreen(viewModel)
                2 -> MetricasSkeleton()
                3 -> AlertasSkeleton()
                4 -> ConfigScreen(viewModel)
            }
        }
    }
}

// --- FASE 2: VISTA GENERAL DE MÉTRICAS ---
@Composable
fun VistaGeneralSkeleton(viewModel: SanaViewModel) {
    val state = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Resumen Operativo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Tarjetas Principales
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = "Total Pacientes",
                value = state.dashTotalCasos.toString(),
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "T. Promedio",
                value = state.dashTiempoPromedio,
                icon = Icons.Default.Monitor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DashboardCard(
            title = "Operadores Activos",
            value = state.dashOperadoresActivos.toString(),
            icon = Icons.Default.HealthAndSafety,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Distribución de Prioridades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Tarjetas de Colores (Triage)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityCard("CRÍTICO", state.dashRojos, Color(0xFFE53935), Modifier.weight(1f))
            PriorityCard("MODERADO", state.dashAmarillos, Color(0xFFFDD835), Modifier.weight(1f))
            PriorityCard("LEVE", state.dashVerdes, Color(0xFF43A047), Modifier.weight(1f))
        }
    }
}

// --- SUB-COMPONENTES DE UI PARA LAS TARJETAS ---
@Composable
fun DashboardCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PriorityCard(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

// --- ESQUELETOS TEMPORALES DEL RESTO DE PESTAÑAS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasosScreen(viewModel: SanaViewModel) {
    val state = viewModel.uiState

    // Lógica para filtrar la lista basándonos en el botón seleccionado
    val casosMostrados = when (state.filtroPrioridadActivo) {
        "CRÍTICO" -> state.dashboardLogs.filter { it.nivelRiesgoAsignado.contains("RED") || it.nivelRiesgoAsignado.contains("EMERGENCIA") }
        "MODERADO" -> state.dashboardLogs.filter { it.nivelRiesgoAsignado.contains("YELLOW") || it.nivelRiesgoAsignado.contains("MODERADO") }
        "LEVE" -> state.dashboardLogs.filter { it.nivelRiesgoAsignado.contains("GREEN") || it.nivelRiesgoAsignado.contains("LEVE") }
        else -> state.dashboardLogs // "TODOS"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- CABECERA Y FILTROS ---
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Registro de Evaluaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Fila de botones para filtrar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val opcionesFiltro = listOf("TODOS", "CRÍTICO", "MODERADO", "LEVE")

                opcionesFiltro.forEach { opcion ->
                    FilterChip(
                        selected = state.filtroPrioridadActivo == opcion,
                        onClick = { viewModel.setFiltroPrioridad(opcion) },
                        label = { Text(opcion, fontSize = 12.sp) }
                    )
                }
            }
        }

        // --- LISTA DE CASOS (TABLA) ---
        if (casosMostrados.isEmpty()) {
            // Pantalla vacía si no hay datos
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay casos registrados en esta categoría.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // Lista deslizable
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(casosMostrados) { caso ->
                    CasoItemCard(caso = caso, onClick = {viewModel.setCasoSeleccionado(caso)})
                }
            }
        }
    }
    // Mostrar Dialog de Detalle si hay un caso seleccionado
    if (state.casoSeleccionadoParaDetalle != null) {
        DetalleCasoDialog(
            caso = state.casoSeleccionadoParaDetalle,
            onDismiss = { viewModel.setCasoSeleccionado(null) }
        )
    }
}

// Componente visual para cada fila de la lista
@Composable
fun CasoItemCard(caso: com.g022.sanamovil.database.ClinicalDecisionLog, onClick: () -> Unit) { // <-- Agregamos onClick
    val colorPrioridad = when {
        caso.nivelRiesgoAsignado.contains("RED") || caso.nivelRiesgoAsignado.contains("EMERGENCIA") -> Color(0xFFE53935)
        caso.nivelRiesgoAsignado.contains("YELLOW") || caso.nivelRiesgoAsignado.contains("MODERADO") -> Color(0xFFFDD835)
        else -> Color(0xFF43A047)
    }

    val fechaFormateada = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(caso.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, // <-- Conectamos el clic aquí
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(16.dp).background(colorPrioridad, shape = androidx.compose.foundation.shape.CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ID: ${caso.caseId.take(8).uppercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Operador: ${caso.idDispositivoOrigen}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fechaFormateada, style = MaterialTheme.typography.labelSmall)
                Text("${caso.tiempoProcesamientoMs / 1000}s de IA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// --- FASE 3.5: VENTANA DE DETALLES DEL CASO ---
@Composable
fun DetalleCasoDialog(
    caso: com.g022.sanamovil.database.ClinicalDecisionLog,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Detalle del Caso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Prioridad: ${caso.nivelRiesgoAsignado}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Síntomas Reportados:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(caso.inputCompleto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))

                Text("Análisis de la IA:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(caso.outputGenerado, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Reglas activadas: ${caso.reglasActivadas}", fontSize = 12.sp, color = Color.Gray)
                Text("T. Procesamiento: ${caso.tiempoProcesamientoMs} ms", fontSize = 12.sp, color = Color.Gray)
                Text("Consentimiento: ${if (caso.consentimientoAceptado) "Aceptado" else "Pendiente"}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// --- FASE 4: CONFIGURACIÓN Y EXPORTACIÓN ---
@Composable
fun ConfigScreen(viewModel: SanaViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importarDatos(context, it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Configuración de Brigada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Sincronización Offline", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Exporta la base de datos local encriptada para enviarla al servidor central, o importa un archivo de respaldo (.sana).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.exportarDatos(context) { uri ->
                    uri?.let {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_STREAM, it)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Enviar Backup SanaMovil"))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exportar Casos Locales")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Importar Base de Datos")
        }
    }
}
@Composable
fun MetricasSkeleton() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Rendimiento Operadores", style = MaterialTheme.typography.headlineSmall) }
}
@Composable
fun AlertasSkeleton() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Alertas del Sistema", style = MaterialTheme.typography.headlineSmall) }
}
@Composable
fun ConfigSkeleton() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Configuración", style = MaterialTheme.typography.headlineSmall) }
}