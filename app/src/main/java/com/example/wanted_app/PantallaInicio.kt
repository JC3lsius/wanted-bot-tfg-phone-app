package com.example.wanted_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaInicio(
    onNavegar: (String) -> Unit = {}
) {
    var busquedas by remember { mutableStateOf<List<BusquedaDto>>(emptyList()) }
    var stats by remember { mutableStateOf<StatsDto?>(null) }
    var cargando by remember { mutableStateOf(true) }

    // Carga inicial: búsquedas (para las activas) + estadísticas (contadores reales).
    LaunchedEffect(Unit) {
        try {
            busquedas = RetrofitCliente.api.getBusquedas()
            stats = RetrofitCliente.api.getStats()
        } catch (_: Exception) {
            // Si el backend no responde, dejamos los valores por defecto (0 / vacío).
        } finally {
            cargando = false
        }
    }

    val activas = busquedas.filter { it.activa }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---------- Cabecera ----------
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("WANTED BOT", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        // ---------- Titular: nº de búsquedas activas (real) ----------
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(
                "${activas.size}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            if (activas.size == 1) "búsqueda activa" else "búsquedas activas",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        // ---------- Fila de estadísticas reales ----------
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaStat("Detectados", stats?.totalProductos ?: 0, Modifier.weight(1f))
            TarjetaStat("Favoritos", stats?.totalFavoritos ?: 0, Modifier.weight(1f))
            TarjetaStat("Comprados", stats?.totalComprados ?: 0, Modifier.weight(1f))
        }

        // ---------- Sección: búsquedas activas ----------
        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Búsquedas activas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { onNavegar("config_bot") }) { Text("Gestionar") }
            }
            Spacer(Modifier.height(4.dp))

            when {
                cargando -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                activas.isEmpty() -> {
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No tienes búsquedas activas",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { onNavegar("config_bot") },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Crear búsqueda")
                            }
                        }
                    }
                }

                else -> {
                    activas.forEach { b ->
                        OutlinedCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Punto verde = activa
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(b.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    if (b.plataformas.isNotEmpty()) {
                                        Text(
                                            b.plataformas.joinToString(", "),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    "Activa",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TarjetaStat(etiqueta: String, valor: Int, modifier: Modifier = Modifier) {
    OutlinedCard(shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$valor", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(etiqueta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
