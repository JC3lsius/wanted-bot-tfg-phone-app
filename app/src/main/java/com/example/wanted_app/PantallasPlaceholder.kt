package com.example.wanted_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// NOTA: la pantalla de Configuración del Bot ahora vive en PantallaConfigBot.kt
// (con el formulario real de creación de búsquedas). Aquí ya no está.

@Composable
fun PantallaBusquedas() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Búsquedas activas", fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PantallaFavoritos(viewModel: ProductosViewModel) {
    val favoritos = viewModel.favoritos

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Favoritos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
        )
        Text(
            "${favoritos.size} guardados",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )

        if (favoritos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No tienes productos guardados",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favoritos) { producto ->
                    TarjetaProductoNueva(
                        producto = producto,
                        onDescartar = { viewModel.descartar(producto.id) },
                        onFavorito = { viewModel.toggleFavorito(producto.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaPerfil() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Perfil", fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PantallaConfigApp(onCerrarSesion: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Configuración de la App",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Sesión iniciada como",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    Sesion.nombre ?: "—",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    Sesion.email ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Refresco automático de productos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Ahora mismo: cada ${Ajustes.intervaloRefrescoSeg} s.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                // Valor en edición (texto para permitir borrar/escribir). No se
                // aplica hasta pulsar "Aplicar".
                var texto by remember { mutableStateOf(Ajustes.intervaloRefrescoSeg.toString()) }
                val valor = texto.toIntOrNull()
                val valido = valor != null && valor in Ajustes.INTERVALO_MIN..Ajustes.INTERVALO_MAX
                val hayCambio = valido && valor != Ajustes.intervaloRefrescoSeg
                val puedeBajar = (valor ?: Ajustes.INTERVALO_MIN) > Ajustes.INTERVALO_MIN
                val puedeSubir = (valor ?: Ajustes.INTERVALO_MAX) < Ajustes.INTERVALO_MAX

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val base = texto.toIntOrNull() ?: Ajustes.intervaloRefrescoSeg
                            texto = (base - 1).coerceIn(Ajustes.INTERVALO_MIN, Ajustes.INTERVALO_MAX).toString()
                        },
                        enabled = puedeBajar
                    ) { Text("−", fontSize = 22.sp) }

                    OutlinedTextField(
                        value = texto,
                        onValueChange = { nuevo -> texto = nuevo.filter { it.isDigit() }.take(2) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(72.dp)
                    )

                    IconButton(
                        onClick = {
                            val base = texto.toIntOrNull() ?: Ajustes.intervaloRefrescoSeg
                            texto = (base + 1).coerceIn(Ajustes.INTERVALO_MIN, Ajustes.INTERVALO_MAX).toString()
                        },
                        enabled = puedeSubir
                    ) { Text("+", fontSize = 22.sp) }

                    Text("s", fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = { valor?.let { Ajustes.fijarIntervalo(it) } },
                        enabled = hayCambio
                    ) { Text("Aplicar") }
                }

                if (texto.isNotEmpty() && !valido) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Debe estar entre ${Ajustes.INTERVALO_MIN} y ${Ajustes.INTERVALO_MAX} s.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = Ajustes.autoScrollNuevos,
                        onCheckedChange = { Ajustes.fijarAutoScroll(it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Saltar a los productos nuevos", fontSize = 13.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Si estás arriba del todo, mantiene la lista en los más recientes al llegar nuevos.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cerrar sesión")
        }
    }
}
