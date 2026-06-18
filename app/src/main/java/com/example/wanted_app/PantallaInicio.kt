package com.example.wanted_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wanted_app.ui.theme.*

@Composable
fun PantallaInicio(
    onNavegar: (String) -> Unit = {}
) {
    var stats by remember { mutableStateOf<StatsDto?>(null) }
    var ultimo by remember { mutableStateOf<Producto?>(null) }
    var cargando by remember { mutableStateOf(true) }

    // Carga datos reales del backend al entrar.
    LaunchedEffect(Unit) {
        try {
            stats = RetrofitCliente.api.getStats()
            val lista = RetrofitCliente.api.getProductos(limite = 1)
            ultimo = lista.firstOrNull()?.let {
                Producto(
                    id = it.id, nombre = it.nombre, precio = it.precio,
                    plataforma = it.plataforma, imagenUrl = it.imagenUrl,
                    enlace = it.enlace, tiempoDetectado = it.fechaDetectado ?: "",
                    esFavorito = it.favorito, busqueda = it.busqueda, imagenes = it.imagenes
                )
            }
        } catch (_: Exception) {
            // Si el backend no responde, dejamos los datos vacíos (se muestra el aviso).
        } finally {
            cargando = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // Avatar + nombre
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Avatar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("WANTED BOT", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            if (Sesion.nombre != null) "Hola, ${Sesion.nombre}" else "Bienvenido",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Estadísticas reales (/stats)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TarjetaStat("Productos", stats?.totalProductos, Modifier.weight(1f))
            TarjetaStat("Favoritos", stats?.totalFavoritos, Modifier.weight(1f))
            TarjetaStat("Búsquedas", stats?.totalBusquedas, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // Último producto DETECTADO (esto sí lo sabemos; lo trae el scraper)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "Último detectado",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            val u = ultimo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                if (u != null) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                u.plataforma.take(1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                u.nombre,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                u.plataforma,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            String.format("%.2f EUR", u.precio),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrecioColor
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (cargando) "Cargando…" else "Aún no hay productos detectados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaStat(etiqueta: String, valor: Int?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                valor?.toString() ?: "—",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
            Spacer(Modifier.height(2.dp))
            Text(etiqueta, fontSize = 11.sp, color = PrimaryDark)
        }
    }
}
