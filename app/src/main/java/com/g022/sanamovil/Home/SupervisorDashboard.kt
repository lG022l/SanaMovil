package com.g022.sanamovil.Home

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
                1 -> CasosSkeleton()
                2 -> MetricasSkeleton()
                3 -> AlertasSkeleton()
                4 -> ConfigSkeleton()
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
@Composable
fun CasosSkeleton() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Lista de Casos", style = MaterialTheme.typography.headlineSmall) }
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