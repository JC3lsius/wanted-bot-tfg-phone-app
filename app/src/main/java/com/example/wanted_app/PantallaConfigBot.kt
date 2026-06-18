package com.example.wanted_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaConfigBot() {
    val scope = rememberCoroutineScope()

    var busquedas by remember { mutableStateOf<List<BusquedaDto>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // ---- Estado del formulario ----
    var nombre by remember { mutableStateOf("") }
    var plataformasSel by remember { mutableStateOf(setOf<String>()) }
    var incluidas by remember { mutableStateOf(listOf<String>()) }
    var excluidas by remember { mutableStateOf(listOf<String>()) }
    var precioMin by remember { mutableStateOf("") }
    var precioMax by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("listener") }
    var profundidad by remember { mutableStateOf(1) }
    var limiteProductos by remember { mutableStateOf(0) }
    var busquedaLimpiar by remember { mutableStateOf<BusquedaDto?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val plataformasDisponibles = listOf("Vinted", "Wallapop", "eBay", "Milanuncios")

    fun recargar() {
        scope.launch {
            cargando = true
            error = null
            try {
                busquedas = RetrofitCliente.api.getBusquedas()
            } catch (e: Exception) {
                error = "No se pudieron cargar las búsquedas: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    busquedaLimpiar?.let { objetivo ->
        AlertDialog(
            onDismissRequest = { busquedaLimpiar = null },
            title = { Text("Vaciar productos") },
            text = { Text("Se eliminarán todos los productos de \"${objetivo.nombre}\". La búsqueda seguirá activa.") },
            confirmButton = {
                TextButton(onClick = {
                    busquedaLimpiar = null
                    scope.launch {
                        try {
                            RetrofitCliente.api.borrarProductos(busqueda = objetivo.nombre)
                        } catch (e: Exception) {
                            error = "No se pudieron borrar los productos: ${e.message}"
                        }
                    }
                }) { Text("Vaciar") }
            },
            dismissButton = {
                TextButton(onClick = { busquedaLimpiar = null }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Configuración del Bot", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            "Crea búsquedas y el bot las ejecutará automáticamente.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // ====================== Formulario nueva búsqueda ======================
        OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Nueva búsqueda", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                Text("Plataformas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plataformasDisponibles.forEach { p ->
                        val sel = p in plataformasSel
                        FilterChip(
                            selected = sel,
                            onClick = {
                                plataformasSel = if (sel) plataformasSel - p else plataformasSel + p
                            },
                            label = { Text(p, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                EntradaEtiquetas(
                    titulo = "Palabras incluidas",
                    etiquetas = incluidas,
                    onAniadir = { incluidas = incluidas + it },
                    onQuitar = { incluidas = incluidas - it }
                )

                Spacer(Modifier.height(12.dp))
                EntradaEtiquetas(
                    titulo = "Palabras excluidas",
                    etiquetas = excluidas,
                    onAniadir = { excluidas = excluidas + it },
                    onQuitar = { excluidas = excluidas - it }
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = precioMin,
                        onValueChange = { nuevo -> precioMin = nuevo.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Precio mín") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = precioMax,
                        onValueChange = { nuevo -> precioMax = nuevo.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Precio máx") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text("Tipo de búsqueda", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipo == "listener",
                        onClick = { tipo = "listener" },
                        label = { Text("Listener", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = tipo == "once",
                        onClick = { tipo = "once" },
                        label = { Text("Una vez", fontSize = 12.sp) }
                    )
                }
                Text(
                    if (tipo == "once") "Hace un barrido y se detiene."
                    else "Sigue vigilando y avisa de lo nuevo.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                Text("Profundidad (páginas por barrido)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = profundidad == 1,
                        onClick = { profundidad = 1 },
                        label = { Text("1 página", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = profundidad == 5,
                        onClick = { profundidad = 5 },
                        label = { Text("5 páginas", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = profundidad == 0,
                        onClick = { profundidad = 0 },
                        label = { Text("Sin límite", fontSize = 12.sp) }
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("Límite de productos por escaneo", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = limiteProductos == 0,
                        onClick = { limiteProductos = 0 },
                        label = { Text("Sin tope", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = limiteProductos == 50,
                        onClick = { limiteProductos = 50 },
                        label = { Text("50", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = limiteProductos == 100,
                        onClick = { limiteProductos = 100 },
                        label = { Text("100", fontSize = 12.sp) }
                    )
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        guardando = true
                        error = null
                        scope.launch {
                            try {
                                RetrofitCliente.api.crearBusqueda(
                                    BusquedaCrearRequest(
                                        nombre = nombre.trim(),
                                        plataformas = plataformasSel.toList(),
                                        palabrasIncluidas = incluidas,
                                        palabrasExcluidas = excluidas,
                                        precioMin = precioMin.toDoubleOrNull() ?: 0.0,
                                        precioMax = precioMax.toDoubleOrNull() ?: 99999.0,
                                        tipo = tipo,
                                        profundidad = profundidad,
                                        limiteProductos = limiteProductos
                                    )
                                )
                                // limpiar formulario
                                nombre = ""
                                plataformasSel = emptySet()
                                incluidas = emptyList()
                                excluidas = emptyList()
                                precioMin = ""
                                precioMax = ""
                                tipo = "listener"
                                profundidad = 1
                                limiteProductos = 0
                                recargar()
                            } catch (e: Exception) {
                                error = "No se pudo crear la búsqueda: ${e.message}"
                            } finally {
                                guardando = false
                            }
                        }
                    },
                    enabled = !guardando && nombre.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (guardando) "Creando..." else "Crear búsqueda")
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ====================== Lista de búsquedas ======================
        Text("Búsquedas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        when {
            cargando -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            error != null -> {
                Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            busquedas.isEmpty() -> {
                Text(
                    "Aún no has creado ninguna búsqueda.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                busquedas.forEach { b ->
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (b.plataformas.isNotEmpty()) {
                                    Text(
                                        b.plataformas.joinToString(", "),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    if (b.activa) "● En ejecución" else "○ Detenida",
                                    fontSize = 11.sp,
                                    color = if (b.activa) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        if (b.activa) RetrofitCliente.api.pararBusqueda(b.id)
                                        else RetrofitCliente.api.iniciarBusqueda(b.id)
                                        recargar()
                                    } catch (e: Exception) {
                                        error = "Error al cambiar el estado: ${e.message}"
                                    }
                                }
                            }) {
                                Text(if (b.activa) "Parar" else "Iniciar", fontSize = 13.sp)
                            }

                            TextButton(onClick = { busquedaLimpiar = b }) {
                                Text("Vaciar", fontSize = 13.sp)
                            }

                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        RetrofitCliente.api.borrarBusqueda(b.id)
                                        recargar()
                                    } catch (e: Exception) {
                                        error = "No se pudo borrar: ${e.message}"
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Borrar",
                                    tint = MaterialTheme.colorScheme.error
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


// ===========================================================================
//  Entrada de etiquetas: escribes una palabra, pulsas Enter (o coma) y se
//  convierte en una burbuja. Tocar la burbuja la elimina.
// ===========================================================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EntradaEtiquetas(
    titulo: String,
    etiquetas: List<String>,
    onAniadir: (String) -> Unit,
    onQuitar: (String) -> Unit,
    placeholder: String = "Escribe y pulsa Enter"
) {
    var texto by remember { mutableStateOf("") }

    fun confirmar() {
        val limpio = texto.trim()
        if (limpio.isNotEmpty() && limpio !in etiquetas) onAniadir(limpio)
        texto = ""
    }

    Column {
        Text(titulo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = texto,
            onValueChange = { nuevo ->
                // Si teclea una coma (o un salto), también confirmamos: es cómodo.
                if (nuevo.endsWith(",") || nuevo.endsWith("\n")) {
                    texto = nuevo.dropLast(1)
                    confirmar()
                } else {
                    texto = nuevo
                }
            },
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { confirmar() })
        )
        if (etiquetas.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                etiquetas.forEach { etiqueta ->
                    InputChip(
                        selected = false,
                        onClick = { onQuitar(etiqueta) },
                        label = { Text(etiqueta, fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
