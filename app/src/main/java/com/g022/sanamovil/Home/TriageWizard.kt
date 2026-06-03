package com.g022.sanamovil.Home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g022.sanamovil.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageWizard(
    uiState: UiState,
    onAgeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onChronicConditionsChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ayúdanos a entender mejor",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Para darte una recomendación segura, necesitamos unos datos rápidos:",
                fontSize = 16.sp
            )

            // 1. Edad (Campo Obligatorio)
            OutlinedTextField(
                value = uiState.wizardAge,
                onValueChange = onAgeChange,
                label = { Text("Edad (años) *Obligatorio*") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. Duración (Dropdown / Chips)
            Text("¿Desde hace cuánto tienes los síntomas?", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Minutos", "Horas", "Días").forEach { option ->
                    FilterChip(
                        selected = uiState.wizardDuration == option,
                        onClick = { onDurationChange(option) },
                        label = { Text(option) }
                    )
                }
            }

            // 3. Enfermedades crónicas o antecedentes (NUEVO)
            Text("Historial médico previo", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.wizardChronicConditions,
                onValueChange = onChronicConditionsChange,
                placeholder = { Text("Ej: Diabetes, hipertensión, asma, marcapasos o 'ninguna'") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp), // Lo hace un poco más alto para invitar a escribir
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )



            Spacer(modifier = Modifier.height(16.dp))

            // 5. Consentimiento (NUEVO)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.wizardConsentAccepted,
                    onCheckedChange = onConsentChange
                )
                Text(
                    text = "Acepto el procesamiento local de mis síntomas para el triage.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de Envío (Con Validación Actualizada)
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                // Lógica actualizada: Obliga a poner edad Y aceptar el consentimiento
                enabled = uiState.wizardAge.isNotEmpty() && uiState.wizardConsentAccepted
            ) {
                Text("Evaluar Síntomas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}