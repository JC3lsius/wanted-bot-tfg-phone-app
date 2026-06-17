package com.example.wanted_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaRegistro(
    onRegistroExitoso: () -> Unit = {},
    onVolverALogin: () -> Unit = {}
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var pass2Visible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo / avatar (mismo estilo que el login)
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("WANTED BOT", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Crea tu cuenta",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // Nombre de usuario
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it; error = null },
            label = { Text("Nombre de usuario") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Correo
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            singleLine = true,
            visualTransformation = if (passVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { passVisible = !passVisible }) {
                    Text(if (passVisible) "Ocultar" else "Ver")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Repetir contraseña
        OutlinedTextField(
            value = password2,
            onValueChange = { password2 = it; error = null },
            label = { Text("Repetir contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            singleLine = true,
            visualTransformation = if (pass2Visible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { pass2Visible = !pass2Visible }) {
                    Text(if (pass2Visible) "Ocultar" else "Ver")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(Modifier.height(24.dp))

        // Botón crear cuenta
        Button(
            onClick = {
                when {
                    nombre.isBlank() || email.isBlank() || password.isBlank() ->
                        error = "Rellena todos los campos"
                    password != password2 ->
                        error = "Las contraseñas no coinciden"
                    password.length < 6 ->
                        error = "La contraseña debe tener al menos 6 caracteres"
                    else -> {
                        // TODO: aquí irá el registro real contra el backend:
                        //   POST /register (Retrofit) con nombre/correo/contraseña.
                        //   Si responde OK, iniciar sesión (o volver al login) y guardar el JWT.
                        // De momento es de solo interfaz para probar la navegación.
                        onRegistroExitoso()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Crear cuenta", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "¿Ya tienes cuenta?",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onVolverALogin) {
                Text("Inicia sesión", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
