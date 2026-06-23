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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import kotlin.math.roundToInt

// Máximo de búsquedas activas simultáneas. Límite "blando" de cliente: súbelo o
// ignóralo desde el futuro modo administrador. El límite "duro" iría en el backend.
const val MAX_BUSQUEDAS_ACTIVAS = 4

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
    var avanzadasAbierto by remember { mutableStateOf(false) }
    var errorForm by remember { mutableStateOf<String?>(null) } 

    val plataformasDisponibles = listOf("Vinted", "Wallapop", "eBay", "Milanuncios")

    val activas = busquedas.count { it.activa }

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

                Spacer(Modifier.height(14.dp))
                Text("Qué buscar", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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

                Spacer(Modifier.height(16.dp))
                Text("Dónde buscar", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plataformasDisponibles.forEach { p ->
                        val sel = p in plataformasSel
                        FilterChip(
                            selected = sel,
                            onClick = {
                                plataformasSel = if (sel) plataformasSel - p else plataformasSel + p
                                if (plataformasSel.isNotEmpty()) errorForm = null
                            },
                            label = { Text(p, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Filtros", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = precioMin,
                        onValueChange = { nuevo -> precioMin = nuevo.filter { c -> c.isDigit() || c == '.' }; errorForm = null },
                        label = { Text("Precio mín") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = precioMax,
                        onValueChange = { nuevo -> precioMax = nuevo.filter { c -> c.isDigit() || c == '.' }; errorForm = null },
                        label = { Text("Precio máx") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { avanzadasAbierto = !avanzadasAbierto },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Opciones avanzadas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (avanzadasAbierto) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (avanzadasAbierto) "Contraer" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = avanzadasAbierto) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text("Tipo de búsqueda", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (tipo == "once") "Barrido exhaustivo: trae todo lo que coincide y se detiene."
                            else "Tiempo real: vigila y avisa solo de lo recién publicado.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Profundidad",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = if (profundidad <= 0) "" else profundidad.toString(),
                                onValueChange = { nuevo ->
                                    val n = nuevo.filter { it.isDigit() }.toIntOrNull()
                                    profundidad = when {
                                        n == null -> 0
                                        n <= 0 -> 0
                                        n > 15 -> 0
                                        else -> n
                                    }
                                },
                                placeholder = { Text("∞") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(110.dp)
                            )
                        }
                        Slider(
                            value = (if (profundidad <= 0) 16 else profundidad.coerceIn(1, 16)).toFloat(),
                            onValueChange = { v ->
                                val iv = v.roundToInt()
                                profundidad = if (iv >= 16) 0 else iv.coerceIn(1, 15)
                            },
                            valueRange = 1f..16f,
                            steps = 14
                        )
                        Text(
                            when {
                                profundidad <= 0 -> "Sin límite (rastrea hasta agotar, máx. 60)"
                                profundidad == 1 -> "1 página / vista (rápido)"
                                else -> "$profundidad páginas o bajadas de scroll por barrido"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Más profundidad = más resultados pero más lento. Solo se nota en el modo 'Una vez'.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                if (activas >= MAX_BUSQUEDAS_ACTIVAS) {
                    Text(
                        "Tienes el máximo de $MAX_BUSQUEDAS_ACTIVAS búsquedas activas. Detén una para crear otra.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        errorForm = null
                        if (plataformasSel.isEmpty()) {
                            errorForm = "Selecciona al menos una plataforma de compraventa"
                            return@Button
                        }
                        // Validación de precio: texto numérico válido y mín <= máx.
                        val min = precioMin.toDoubleOrNull()
                        val max = precioMax.toDoubleOrNull()
                        if (precioMin.isNotBlank() && min == null) {
                            errorForm = "El precio mínimo no es un número válido"
                            return@Button
                        }
                        if (precioMax.isNotBlank() && max == null) {
                            errorForm = "El precio máximo no es un número válido"
                            return@Button
                        }
                        if (min != null && max != null && min > max) {
                            errorForm = "El precio mínimo no puede ser mayor que el máximo"
                            return@Button
                        }
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
                    enabled = !guardando && nombre.isNotBlank() && activas < MAX_BUSQUEDAS_ACTIVAS,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (guardando) "Creando..." else "Crear búsqueda")
                }

                if (errorForm != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorForm!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ====================== Lista de búsquedas ======================
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Búsquedas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "En ejecución: $activas / $MAX_BUSQUEDAS_ACTIVAS",
                fontSize = 12.sp,
                color = if (activas >= MAX_BUSQUEDAS_ACTIVAS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            if (b.activa) RetrofitCliente.api.pararBusqueda(b.id)
                                            else RetrofitCliente.api.iniciarBusqueda(b.id)
                                            recargar()
                                        } catch (e: Exception) {
                                            error = "Error al cambiar el estado: ${e.message}"
                                        }
                                    }
                                },
                                enabled = b.activa || activas < MAX_BUSQUEDAS_ACTIVAS
                            ) {
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
