package com.g022.sanamovil.Auth

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.g022.sanamovil.R
import com.g022.sanamovil.ViewModel.SanaViewModel

@Composable
fun LoginScreen(
    viewModel: SanaViewModel,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    isModelDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onCheckModel: (Context) -> Unit,
    onDownloadModel: (Context) -> Unit
) {
    val context = LocalContext.current
    // --- NUEVO: Instancia de SharedPreferences ---
    val sharedPreferences = context.getSharedPreferences("SanaPrefs", Context.MODE_PRIVATE)

    // --- MODIFICADO: Leen los datos guardados por defecto ---
    var email by rememberSaveable { mutableStateOf(sharedPreferences.getString("email", "") ?: "") }
    var password by rememberSaveable { mutableStateOf(sharedPreferences.getString("password", "") ?: "") }
    var rememberMe by rememberSaveable { mutableStateOf(sharedPreferences.getBoolean("rememberMe", false)) }

    var showPassword by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val logoPainter = painterResource(id = R.drawable.logov3)

    LaunchedEffect(Unit) {
        onCheckModel(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = logoPainter,
            contentDescription = "Logo de la app",
            modifier = Modifier.size(300.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Correo electrónico", color = MaterialTheme.colorScheme.primary) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground), singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Contraseña", color = MaterialTheme.colorScheme.primary) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                val icon = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { showPassword = !showPassword }) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground), visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
        )

        // --- NUEVO: Checkbox de Recuérdame ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Text("Recuérdame", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (mensajeError.isNotEmpty()) {
            Text(
                text = mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                focusManager.clearFocus()

                // Validación básica local (opcional, la dejé para que no rompa flujos si requiere el dato)
                if (email.isEmpty() || password.isEmpty()) {
                    mensajeError = "Ingresa cualquier dato, el backend está bypasseado."
                    return@Button
                }

                // BYPASS ACTIVO: Omitimos la llamada al ViewModel y simulamos éxito inmediato.
                /*
                isLoading = true
                mensajeError = ""

                viewModel.iniciarSesionEnNube(
                    correo = email,
                    contrasena = password,
                    onExito = { ... },
                    onError = { ... }
                )
                */

                // 1. Guardar preferencias locales (mantenemos la lógica de UI intacta)
                with(sharedPreferences.edit()) {
                    if (rememberMe) {
                        putString("email", email)
                        putString("password", password)
                        putBoolean("rememberMe", true)
                    } else {
                        clear()
                    }
                    apply()
                }

                // 2. Disparar el login exitoso directamente
                onLoginSuccess()
            },
            enabled = isModelDownloaded && !isDownloading, // Mantenemos la regla del modelo local
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (!isModelDownloaded) {
                Text("Descarga el modelo para entrar")
            } else if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Iniciar sesión .")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onRegisterClick) { Text("¿No tienes una cuenta?\nRegístrate aquí", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center) }
            TextButton(onClick = onForgotPasswordClick) { Text("¿Olvidaste tu contraseña?", color = MaterialTheme.colorScheme.primary) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bloque de descarga del modelo intacto...
        if (isDownloading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Descargando modelo: ${(downloadProgress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        } else if (!isModelDownloaded) {
            Button(
                onClick = { onDownloadModel(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Descargar Modelo IA Médico")
            }
        } else {
            Text(
                text = "✓ Modelo IA instalado y listo",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        }
    }
}