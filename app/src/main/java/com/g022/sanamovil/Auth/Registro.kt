package com.g022.sanamovil.Auth

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.g022.sanamovil.ViewModel.SanaViewModel // <-- IMPORTANTE: Asegúrate de que esta ruta sea la tuya

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: SanaViewModel, // <-- NUEVO: Recibimos el ViewModel
    onBackToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit // <-- NUEVO: Qué hacer si sale bien
) {
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    var confirmarContraseña by remember { mutableStateOf("") }

    // --- NUEVOS ESTADOS PARA SUPABASE ---
    var isLoading by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Registro",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackToLogin() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Crea tu cuenta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Completa tus datos para comenzar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // --- CAMPOS DE TEXTO (Iguales a los tuyos) ---
            RoundedInputField(
                value = nombre, onValueChange = { nombre = it }, placeholder = "Nombre",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            RoundedInputField(
                value = apellidoPaterno, onValueChange = { apellidoPaterno = it }, placeholder = "Apellido paterno",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            RoundedInputField(
                value = apellidoMaterno, onValueChange = { apellidoMaterno = it }, placeholder = "Apellido materno",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            RoundedInputField(
                value = correo, onValueChange = { correo = it }, placeholder = "Correo electrónico",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            RoundedInputField(
                value = contraseña, onValueChange = { contraseña = it }, placeholder = "Contraseña", isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            RoundedInputField(
                value = confirmarContraseña, onValueChange = { confirmarContraseña = it }, placeholder = "Confirmar contraseña", isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- NUEVO: MOSTRAR ERRORES ---
            if (mensajeError.isNotEmpty()) {
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // --- BOTÓN ACTUALIZADO ---
            Button(
                onClick = {
                    focusManager.clearFocus() // Ocultar teclado

                    // 1. Validaciones locales
                    if (nombre.isEmpty() || correo.isEmpty() || contraseña.isEmpty()) {
                        mensajeError = "Por favor llena todos los campos obligatorios."
                        return@Button
                    }
                    if (contraseña != confirmarContraseña) {
                        mensajeError = "Las contraseñas no coinciden."
                        return@Button
                    }
                    if (contraseña.length < 6) {
                        mensajeError = "La contraseña debe tener al menos 6 caracteres."
                        return@Button
                    }

                    // 2. Enviar a Supabase
                    isLoading = true
                    mensajeError = ""

                    viewModel.registrarUsuarioEnNube(
                        correo = correo,
                        contrasena = contraseña,
                        onExito = {
                            isLoading = false
                            onRegisterSuccess() // Mandar a la pantalla de éxito o login
                        },
                        onError = { error ->
                            isLoading = false
                            mensajeError = error // Mostrar por qué falló
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading, // Se deshabilita mientras carga
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                // Si está cargando, muestra la ruedita. Si no, muestra el texto.
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Registrarse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { onBackToLogin() }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                Text("Cancelar y volver al inicio", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ... Tu Composable RoundedInputField se queda EXACTAMENTE igual ...
@Composable
fun RoundedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}