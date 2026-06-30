package com.example.wanted_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import kotlin.math.roundToInt

// Máximo de búsquedas activas simultáneas. Límite "blando" de cliente: súbelo o
// ignóralo desde el futuro modo administrador. El límite "duro" iría en el backend.
const val MAX_BUSQUEDAS_ACTIVAS = 4

// Formatea un precio para mostrarlo en el formulario: sin decimales si es entero.
private fun fmtPrecio(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaConfigBot() {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
    var omitirPrimera by remember { mutableStateOf(true) }
    var busquedaLimpiar by remember { mutableStateOf<BusquedaDto?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var avanzadasAbierto by remember { mutableStateOf(false) }

    // Estado del panel de creación/edición:
    //  - formAbierto: si el formulario está desplegado.
    //  - editando: la búsqueda que se está modificando (null = crear una nueva).
    var formAbierto by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<BusquedaDto?>(null) }

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

    fun limpiarFormulario() {
        nombre = ""
        plataformasSel = emptySet()
        incluidas = emptyList()
        excluidas = emptyList()
        precioMin = ""
        precioMax = ""
        tipo = "listener"
        profundidad = 1
        limiteProductos = 0
        omitirPrimera = true
        avanzadasAbierto = false
    }

    fun cargarEnFormulario(b: BusquedaDto) {
        nombre = b.nombre
        plataformasSel = b.plataformas.toSet()
        incluidas = b.palabrasIncluidas
        excluidas = b.palabrasExcluidas
        precioMin = if (b.precioMin <= 0.0) "" else fmtPrecio(b.precioMin)
        precioMax = if (b.precioMax <= 0.0 || b.precioMax >= 99999.0) "" else fmtPrecio(b.precioMax)
        tipo = b.tipo
        profundidad = b.profundidad
        limiteProductos = b.limiteProductos
        omitirPrimera = b.omitirPrimera
        // Si trae opciones no básicas, abrimos avanzadas para que se vean.
        avanzadasAbierto = (b.tipo != "listener" || b.profundidad != 1 || b.limiteProductos > 0 || !b.omitirPrimera)
    }

    LaunchedEffect(Unit) { recargar() }

    // Al entrar en modo edición, el formulario se despliega abajo: bajamos hasta él.
    LaunchedEffect(editando?.id, formAbierto) {
        if (formAbierto && editando != null) {
            delay(200)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

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
                            RetrofitCliente.api.borrarProductos(busquedaId = objetivo.id)
                            recargar()
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
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Configuración del Bot", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            "Gestiona tus búsquedas; el bot las ejecuta automáticamente.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // ====================== Lista de búsquedas (arriba) ======================
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
                    "Aún no has creado ninguna búsqueda. Usa «Nueva búsqueda» para empezar.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                busquedas.forEach { b ->
                    val enEdicion = editando?.id == b.id
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 6.dp)) {
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
                            if (enEdicion) {
                                Text(
                                    "Editando esta búsqueda abajo",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            // Acciones. FlowRow para que no desborden en pantallas estrechas.
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(onClick = {
                                    cargarEnFormulario(b)
                                    editando = b
                                    formAbierto = true
                                }) {
                                    Text("Modificar", fontSize = 13.sp)
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

                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            RetrofitCliente.api.borrarBusqueda(b.id)
                                            // Si borramos la que estábamos editando, cerramos el form.
                                            if (editando?.id == b.id) {
                                                editando = null
                                                formAbierto = false
                                                limpiarFormulario()
                                            }
                                            recargar()
                                        } catch (e: Exception) {
                                            error = "No se pudo borrar: ${e.message}"
                                        }
                                    }
                                }) {
                                    Text("Eliminar", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ============ Cabecera plegable del formulario (crear / editar) ============
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (formAbierto) {
                        // Cerrar: si estábamos editando, se cancela la edición.
                        formAbierto = false
                        editando = null
                        limpiarFormulario()
                    } else {
                        // Abrir en modo "crear".
                        editando = null
                        limpiarFormulario()
                        formAbierto = true
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!formAbierto) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                if (editando != null) "Editando: ${editando!!.nombre}" else "Nueva búsqueda",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (formAbierto) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (formAbierto) "Contraer" else "Expandir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = formAbierto) {
            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                    onClick = { tipo = "listener"; limiteProductos = 0; if (omitirPrimera) profundidad = 1 },
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

                            // Solo en Listener: ¿avisar de lo ya publicado en la 1ª pasada
                            // o solo de lo nuevo? Si se omite, la profundidad no aplica.
                            if (tipo == "listener") {
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Omitir productos de la primera búsqueda",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            if (omitirPrimera)
                                                "Solo avisa de lo nuevo a partir de ahora."
                                            else
                                                "Primero trae lo que ya hay (según la profundidad) y luego sigue avisando.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Switch(
                                        checked = omitirPrimera,
                                        onCheckedChange = {
                                            omitirPrimera = it
                                            if (it) profundidad = 1
                                        }
                                    )
                                }
                            }

                            // La profundidad se muestra en "Una vez" y en "Listener"
                            // cuando NO se omiten los productos de la primera búsqueda.
                            if (tipo == "once" || (tipo == "listener" && !omitirPrimera)) {
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
                                "Más profundidad = más resultados, pero más lento.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            }   // <-- cierra el if de la Profundidad
                            // Límite de productos: solo tiene efecto en modo "Una vez".
                            if (tipo == "once") {
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Límite de productos",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = if (limiteProductos <= 0) "" else limiteProductos.toString(),
                                        onValueChange = { nuevo ->
                                            limiteProductos = nuevo.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(0) ?: 0
                                        },
                                        placeholder = { Text("∞") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (limiteProductos <= 0) "Sin límite: trae todo lo que coincida."
                                    else "Máximo $limiteProductos producto(s) por plataforma en el barrido.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (editando == null && activas >= MAX_BUSQUEDAS_ACTIVAS) {
                        Text(
                            "Tienes el máximo de $MAX_BUSQUEDAS_ACTIVAS búsquedas activas. Detén una para crear otra.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            guardando = true
                            error = null
                            val req = BusquedaCrearRequest(
                                nombre = nombre.trim(),
                                plataformas = plataformasSel.toList(),
                                palabrasIncluidas = incluidas,
                                palabrasExcluidas = excluidas,
                                precioMin = precioMin.toDoubleOrNull() ?: 0.0,
                                precioMax = precioMax.toDoubleOrNull() ?: 99999.0,
                                estadoArticulo = editando?.estadoArticulo ?: "",
                                ubicacion = editando?.ubicacion ?: "",
                                categoria = editando?.categoria ?: "",
                                tipo = tipo,
                                profundidad = profundidad,
                                limiteProductos = limiteProductos,
                                omitirPrimera = omitirPrimera
                            )
                            val editandoActual = editando
                            scope.launch {
                                try {
                                    if (editandoActual != null) {
                                        RetrofitCliente.api.actualizarBusqueda(editandoActual.id, req)
                                        // Si la búsqueda estaba en ejecución, modificar sus
                                        // parámetros la deja detenida: el usuario debe volver a
                                        // iniciarla para que el scraper aplique los nuevos valores.
                                        if (editandoActual.activa) {
                                            RetrofitCliente.api.pararBusqueda(editandoActual.id)
                                        }
                                    } else {
                                        RetrofitCliente.api.crearBusqueda(req)
                                    }
                                    limpiarFormulario()
                                    editando = null
                                    formAbierto = false
                                    recargar()
                                } catch (e: Exception) {
                                    error = if (editandoActual != null)
                                        "No se pudo guardar la búsqueda: ${e.message}"
                                    else
                                        "No se pudo crear la búsqueda: ${e.message}"
                                } finally {
                                    guardando = false
                                }
                            }
                        },
                        enabled = !guardando && nombre.isNotBlank() &&
                            (editando != null || activas < MAX_BUSQUEDAS_ACTIVAS),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            when {
                                guardando && editando != null -> "Guardando..."
                                guardando -> "Creando..."
                                editando != null -> "Guardar cambios"
                                else -> "Crear búsqueda"
                            }
                        )
                    }

                    if (editando != null) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                editando = null
                                limpiarFormulario()
                                formAbierto = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar edición")
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
