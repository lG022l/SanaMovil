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
import com.g022.sanamovil.OperadorStats
import com.g022.sanamovil.AlertaSana
import com.g022.sanamovil.AlertaNivel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboard(
    viewModel: SanaViewModel,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    // 👇 NUEVO: Obtenemos el contexto actual de la pantalla
    val context = LocalContext.current

    // 👇 NUEVO: Forzamos la carga de métricas y casos apenas entremos al modo Supervisor
    LaunchedEffect(Unit) {
        viewModel.cargarMetricasDashboard(context)
        viewModel.cargarListaDeCasos(context)
    }

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
                    containerColor = MaterialTheme.colorScheme.primary
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
                2 -> MetricasScreen(viewModel)
                3 -> AlertasScreen(viewModel)
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


// --- FASE 4 Y 6: CONFIGURACIÓN Y EXPORTACIÓN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: SanaViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state = viewModel.uiState

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importarDatos(context, it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Configuración de Despliegue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        //Text("deployment_config.json", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // --- CAMPOS DE CONFIGURACIÓN ---
        OutlinedTextField(
            value = state.configNombreBrigada,
            onValueChange = { viewModel.updateConfigNombre(it) },
            label = { Text("Nombre de la Brigada") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.configContactoEmergencia,
            onValueChange = { viewModel.updateConfigContacto(it) },
            label = { Text("Radio / Contacto Central") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Nivel de Recursos:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val opciones = listOf("Básico", "Intermedio", "Avanzado")
            opciones.forEach { opcion ->
                FilterChip(
                    selected = state.configNivelRecursos == opcion,
                    onClick = { viewModel.updateConfigNivel(opcion) },
                    label = { Text(opcion) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.guardarConfiguracionLocal(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar Parámetros")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        // --- EXPORTACIÓN E IMPORTACIÓN ---
        Text("Sincronización Offline", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

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
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exportar Casos Locales (AES)")
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
// --- FASE 5: MÉTRICAS DE OPERADORES ---
@Composable
fun MetricasScreen(viewModel: SanaViewModel) {
    // Si la base de datos ya tiene datos reales del motor, los usa.
    // Si está vacía (porque aún no hay login), inyectamos datos falsos (MOCK) para ver el diseño.
    val statsReales = viewModel.uiState.metricasPorOperador

    val operadoresAMostrar = if (statsReales.isNotEmpty()) {
        statsReales.values.toList()
    } else {
        // DATOS FALSOS PARA PREVISUALIZAR EL DISEÑO
        listOf(
            com.g022.sanamovil.OperadorStats(
                nombre = "Dr. Víctor Guzmán", totalPacientes = 45, tiempoPromedioMs = 125000, porcIncompletos = 2f,
                distribucionRiesgo = mapOf("CRÍTICO" to 10, "MODERADO" to 20, "LEVE" to 15)
            ),
            com.g022.sanamovil.OperadorStats(
                nombre = "Enf. María López", totalPacientes = 82, tiempoPromedioMs = 85000, porcIncompletos = 0f,
                distribucionRiesgo = mapOf("CRÍTICO" to 5, "MODERADO" to 30, "LEVE" to 47)
            ),
            com.g022.sanamovil.OperadorStats(
                nombre = "Paramédico Carlos R.", totalPacientes = 12, tiempoPromedioMs = 210000, porcIncompletos = 15f,
                distribucionRiesgo = mapOf("CRÍTICO" to 8, "MODERADO" to 2, "LEVE" to 2)
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Rendimiento del Personal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Basado en el historial de triage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(operadoresAMostrar) { operador ->
            OperadorCard(operador)
        }
    }
}

@Composable
fun OperadorCard(op: com.g022.sanamovil.OperadorStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECERA: Nombre y Total de pacientes
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(op.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text("${op.totalPacientes} Casos", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 1: Velocidad (Tiempo Promedio)
            val segundos = op.tiempoPromedioMs / 1000
            val minutos = segundos / 60
            val segsRestantes = segundos % 60
            val textoTiempo = if(minutos > 0) "${minutos}m ${segsRestantes}s" else "${segsRestantes}s"

            // Si tarda más de 3 minutos (180s), se pinta de rojo
            val colorTiempo = if (segundos > 180) Color(0xFFE53935) else MaterialTheme.colorScheme.primary

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tiempo Promedio:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(textoTiempo, style = MaterialTheme.typography.labelLarge, color = colorTiempo, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { (segundos / 300f).coerceAtMost(1f) }, // Límite visual de 5 minutos
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp).height(6.dp),
                color = colorTiempo,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // SECCIÓN 2: Distribución de Triage
            Text("Tendencia de Diagnósticos:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))

            val rojos = op.distribucionRiesgo["CRÍTICO"] ?: 0
            val amarillos = op.distribucionRiesgo["MODERADO"] ?: 0
            val verdes = op.distribucionRiesgo["LEVE"] ?: 0

            BarraDistribucion(rojos, amarillos, verdes, op.totalPacientes)
        }
    }
}

// COMPONENTE: Una barra segmentada muy visual
@Composable
fun BarraDistribucion(rojos: Int, amarillos: Int, verdes: Int, total: Int) {
    if (total == 0) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
        ) {
            if (rojos > 0) Box(modifier = Modifier.weight(rojos.toFloat()).fillMaxHeight().background(Color(0xFFE53935)))
            if (amarillos > 0) Box(modifier = Modifier.weight(amarillos.toFloat()).fillMaxHeight().background(Color(0xFFFDD835)))
            if (verdes > 0) Box(modifier = Modifier.weight(verdes.toFloat()).fillMaxHeight().background(Color(0xFF43A047)))
        }

        // Leyenda chiquita debajo de la barra
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$rojos Críticos", fontSize = 10.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
            Text("$amarillos Moderados", fontSize = 10.sp, color = Color(0xFFFDD835).copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            Text("$verdes Leves", fontSize = 10.sp, color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
fun AlertasScreen(viewModel: SanaViewModel) {
    val alertas = viewModel.uiState.alertasSistema

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Centro de Notificaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (alertas.isEmpty()) {
            Text("✅ No se detectan anomalías en el sistema.", color = Color(0xFF43A047))
        } else {
            alertas.forEach { alerta ->
                val color = when(alerta.nivel) {
                    AlertaNivel.CRITICAL -> Color(0xFFE53935)
                    AlertaNivel.WARNING -> Color(0xFFFDD835)
                    else -> MaterialTheme.colorScheme.primary
                }

                ListItem(
                    headlineContent = { Text(alerta.titulo, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(alerta.descripcion) },
                    leadingContent = { Icon(Icons.Default.Warning, contentDescription = null, tint = color) },
                    colors = ListItemDefaults.colors(containerColor = color.copy(alpha = 0.1f))
                )
                HorizontalDivider()
            }
        }
    }
}
@Composable
fun ConfigSkeleton() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Configuración", style = MaterialTheme.typography.headlineSmall) }
}