package com.example.wanted_app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.rememberLazyListState
import coil.compose.AsyncImage
import com.example.wanted_app.ui.theme.*
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import kotlinx.coroutines.delay

data class PlataformaInfo(
    val nombre: String,
    val colorFondo: Color,
    val colorTexto: Color
)

val plataformas = mapOf(
    "Vinted" to PlataformaInfo("Vinted", VintedBg, VintedText),
    "Wallapop" to PlataformaInfo("Wallapop", WallapopBg, WallapopText),
    "eBay" to PlataformaInfo("eBay", EbayBg, EbayText),
    "Milanuncios" to PlataformaInfo("Milanuncios", MilanunciosBg, MilanunciosText)
)

private val TARJETA_ALTURA = 130.dp

@Composable
fun PantallaProductos(viewModel: ProductosViewModel) {
    var vistaCompacta by remember { mutableStateOf(false) }
    var mostrarBusquedas by remember { mutableStateOf(false) }
    var busquedasSeleccionadas by remember { mutableStateOf(setOf<Int>()) }
    var plataformasSeleccionadas by remember { mutableStateOf(setOf<String>()) }
    var textoBusquedaManual by remember { mutableStateOf("") }
    var soloFavoritos by remember { mutableStateOf(false) }
    var ordenCampo by remember { mutableStateOf("fecha") }   // "fecha" o "precio"
    var ordenAsc by remember { mutableStateOf(false) }       // false = descendente
    var mostrarConfirmarBorrado by remember { mutableStateOf(false) }

    val productosList = viewModel.productos

    // Cargamos las búsquedas reales del backend para poder mostrar su NOMBRE
    // (el campo producto.busqueda trae la URL, no sirve para el filtro).
    var busquedasBackend by remember { mutableStateOf<List<BusquedaDto>>(emptyList()) }

    // Recarga búsquedas + primera página al entrar (p. ej. tras crear una búsqueda).
    LaunchedEffect(Unit) {
        try { busquedasBackend = RetrofitCliente.api.getBusquedas() } catch (_: Exception) {}
        viewModel.cargarProductos()
    }

    // Mapa de busquedaId → nombre legible
    val mapaBusquedas: Map<Int, String> = busquedasBackend.associate { it.id to it.nombre }

    // Las "búsquedas" del filtro son los IDs presentes en los productos
    val todasBusquedas = productosList.mapNotNull { it.busquedaId }.distinct()
    val todasPlataformas = productosList.map { it.plataforma }.distinct()

    val productosFiltrados = productosList
        .filter { producto ->
            val pasaBusqueda = busquedasSeleccionadas.isEmpty() ||
                (producto.busquedaId != null && producto.busquedaId in busquedasSeleccionadas)
            val pasaPlataforma = plataformasSeleccionadas.isEmpty() || producto.plataforma in plataformasSeleccionadas
            val pasaFavorito = !soloFavoritos || producto.esFavorito
            pasaBusqueda && pasaPlataforma && pasaFavorito
        }
        .let { lista ->
            val ordenada = when (ordenCampo) {
                "precio" -> lista.sortedBy { it.precio }
                else -> lista.sortedBy { it.tiempoDetectado }   // fecha ISO: ordena como texto = cronológico
            }
            if (ordenAsc) ordenada else ordenada.reversed()
        }

    val listState = rememberLazyListState()
    // Cuando el usuario se acerca al final de la lista, pedimos la siguiente página.
    val cargarMasAhora by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val ultimo = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && ultimo >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(cargarMasAhora) {
        if (cargarMasAhora) viewModel.cargarMas()
    }

    // Auto-refresco: mientras esta pantalla está visible, cada intervalo (Ajustes)
    // trae los productos nuevos sin recargar a mano. Se cancela al salir de la
    // pantalla. Si llegan novedades y el usuario está justo arriba del todo, sube
    // la lista para enseñárselas; si está desplazado, se mantiene en su sitio.
    LaunchedEffect(Unit) {
        while (true) {
            delay(Ajustes.intervaloRefrescoSeg * 1000L)
            val estabaArriba = listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
            val nuevos = viewModel.refrescarSilencioso()
            if (nuevos > 0 && estabaArriba) listState.animateScrollToItem(0)
        }
    }

    if (mostrarConfirmarBorrado) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarBorrado = false },
            title = { Text("Borrar todos los productos") },
            text = { Text("Se eliminarán todos los productos detectados. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmarBorrado = false
                    viewModel.borrarTodos()
                }) { Text("Borrar todo") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarBorrado = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Productos", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mostrarConfirmarBorrado = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Borrar todos los productos",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vistaCompacta = !vistaCompacta }) {
                    Icon(
                        if (vistaCompacta) Icons.Default.List else Icons.Default.Menu,
                        contentDescription = "Cambiar vista",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Zona de filtros + lista con dropdown superpuesto
        Box(modifier = Modifier.fillMaxSize()) {
            // Contenido principal
            Column(modifier = Modifier.fillMaxSize()) {
                // Fila de filtros compacta
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val numSeleccionadas = busquedasSeleccionadas.size
                    val textoChip = if (numSeleccionadas == 0) "Busquedas"
                        else "Busquedas ($numSeleccionadas/${todasBusquedas.size})"

                    Row(
                        modifier = Modifier
                            .background(
                                if (numSeleccionadas > 0) PrimaryLight
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { mostrarBusquedas = !mostrarBusquedas }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (mostrarBusquedas) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (numSeleccionadas > 0) PrimaryDark
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            textoChip,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (numSeleccionadas > 0) PrimaryDark
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(todasPlataformas) { plat ->
                            val info = plataformas[plat]
                            val seleccionada = plat in plataformasSeleccionadas
                            if (info != null) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (seleccionada) info.colorFondo
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .then(
                                            if (seleccionada) Modifier.border(2.dp, info.colorTexto, CircleShape)
                                            else Modifier
                                        )
                                        .clickable {
                                            plataformasSeleccionadas = if (seleccionada)
                                                plataformasSeleccionadas - plat
                                            else
                                                plataformasSeleccionadas + plat
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        plat.first().toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (seleccionada) info.colorTexto
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Fila: favoritos + orden
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (soloFavoritos) PrimaryLight
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { soloFavoritos = !soloFavoritos }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (soloFavoritos) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (soloFavoritos) Favorito
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Favoritos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (soloFavoritos) PrimaryDark
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .clickable { ordenCampo = if (ordenCampo == "fecha") "precio" else "fecha" }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (ordenCampo == "precio") "Precio" else "Fecha",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { ordenAsc = !ordenAsc }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (ordenAsc) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dirección de orden",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Contador
                Text(
                    "${productosFiltrados.size} productos encontrados",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )

                // Lista
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (vistaCompacta) 4.dp else 10.dp),
                    modifier = Modifier.animateContentSize()
                ) {
                    items(productosFiltrados, key = { it.id }) { producto ->
                        val nombreBusqueda = producto.busquedaId?.let { mapaBusquedas[it] } ?: ""
                        if (vistaCompacta) {
                            TarjetaCompacta(
                                producto = producto,
                                onDescartar = { viewModel.descartar(producto.id) },
                                onFavorito = { viewModel.toggleFavorito(producto.id) },
                                nombreBusqueda = nombreBusqueda
                            )
                        } else {
                            TarjetaProductoNueva(
                                producto = producto,
                                onDescartar = { viewModel.descartar(producto.id) },
                                onFavorito = { viewModel.toggleFavorito(producto.id) },
                                nombreBusqueda = nombreBusqueda
                            )
                        }
                    }

                    if (viewModel.cargandoMas) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }
                }
            }

            // Dropdown flotante superpuesto
            if (mostrarBusquedas) {
                // Fondo para cerrar al tocar fuera
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { mostrarBusquedas = false }
                )

                // Panel flotante
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 40.dp)
                        .zIndex(2f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Campo de busqueda manual
                        OutlinedTextField(
                            value = textoBusquedaManual,
                            onValueChange = { textoBusquedaManual = it },
                            placeholder = {
                                Text("Buscar producto...", fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (textoBusquedaManual.isNotBlank()) {
                                    IconButton(
                                        onClick = { textoBusquedaManual = "" },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Lista de busquedas con checkboxes.
                        // Filtramos por el NOMBRE legible (no por el id).
                        val busquedasFiltradas = todasBusquedas.filter { id ->
                            val nombre = mapaBusquedas[id] ?: ""
                            textoBusquedaManual.isBlank() ||
                                    nombre.contains(textoBusquedaManual, ignoreCase = true)
                        }
                        busquedasFiltradas.forEach { busquedaId ->
                            val nombre = mapaBusquedas[busquedaId] ?: "Búsqueda $busquedaId"
                            val seleccionada = busquedaId in busquedasSeleccionadas
                            val count = productosList.count { it.busquedaId == busquedaId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (seleccionada) PrimaryLight.copy(alpha = 0.5f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        busquedasSeleccionadas = if (seleccionada)
                                            busquedasSeleccionadas - busquedaId
                                        else
                                            busquedasSeleccionadas + busquedaId
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = seleccionada,
                                    onCheckedChange = {
                                        busquedasSeleccionadas = if (seleccionada)
                                            busquedasSeleccionadas - busquedaId
                                        else
                                            busquedasSeleccionadas + busquedaId
                                    },
                                    modifier = Modifier.size(18.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrecioColor,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                                Text(
                                    nombre,
                                    fontSize = 13.sp,
                                    fontWeight = if (seleccionada) FontWeight.Medium else FontWeight.Normal,
                                    color = if (seleccionada) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$count",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaProductoNueva(producto: Producto, onDescartar: () -> Unit, onFavorito: () -> Unit, nombreBusqueda: String = "") {
    val info = plataformas[producto.plataforma]
    var mostrarChat by remember { mutableStateOf(false) }
    var mostrarGaleria by remember { mutableStateOf(false) }
    var botonActivo by remember { mutableStateOf<String?>(null) }

    if (mostrarGaleria) {
        Dialog(onDismissRequest = { mostrarGaleria = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(producto.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (producto.imagenUrl.isNotBlank()) {
                            AsyncImage(
                                model = producto.imagenUrl,
                                contentDescription = producto.nombre,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Sin imagen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(producto.imagenes) { img ->
                            AsyncImage(
                                model = img,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${producto.imagenes.size} fotos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (mostrarChat) {
        Dialog(onDismissRequest = { mostrarChat = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    TextButton(
                        onClick = { mostrarChat = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Negociar precio m\u00e1s bajo", fontSize = 14.sp)
                    }
                    HorizontalDivider()
                    TextButton(
                        onClick = { mostrarChat = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Preguntar acerca del producto", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(TARJETA_ALTURA)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { botonActivo = null },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (info != null) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .fillMaxHeight()
                        .background(
                            info.colorFondo,
                            RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        info.nombre,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = info.colorTexto,
                        modifier = Modifier.textoVertical(),
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(TARJETA_ALTURA)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        botonActivo = null
                        mostrarGaleria = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (producto.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = producto.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        producto.plataforma.firstOrNull()?.toString() ?: "?",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (producto.imagenes.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                Color.Black.copy(alpha = 0.55f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${producto.imagenes.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            producto.nombre,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            String.format("%.2f EUR", producto.precio),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (MaterialTheme.colorScheme.background == BackgroundDark)
                                PrecioColorDark else PrecioColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clickable { botonActivo = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("i", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column {
                    Text(
                        nombreBusqueda.ifBlank { producto.busqueda },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorito
                        FilledTonalIconButton(
                            onClick = { botonActivo = null; onFavorito() },
                            modifier = Modifier.size(34.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (producto.esFavorito) Color(0xFFFCEBEB) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                if (producto.esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (producto.esFavorito) Favorito else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalIconButton(
                            onClick = { botonActivo = null; mostrarChat = true },
                            modifier = Modifier.size(34.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SecondaryLight)
                        ) {
                            Text("\u2026", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Secondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        val descartarActivo = botonActivo == "descartar"
                        FilledTonalIconButton(
                            onClick = {
                                if (descartarActivo) { botonActivo = null; onDescartar() }
                                else { botonActivo = "descartar" }
                            },
                            modifier = Modifier.size(34.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (descartarActivo) Color(0xFFE24B4A).copy(alpha = 0.3f) else Color(0xFFFCEBEB)
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Descartar",
                                tint = if (descartarActivo) Color(0xFFE24B4A) else Favorito.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        val comprarActivo = botonActivo == "comprar"
                        FilledTonalIconButton(
                            onClick = {
                                if (comprarActivo) { botonActivo = null }
                                else { botonActivo = "comprar" }
                            },
                            modifier = Modifier.size(34.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (comprarActivo) Color(0xFF0F6E56).copy(alpha = 0.3f) else VintedBg
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Comprar",
                                tint = if (comprarActivo) Color(0xFF0F6E56) else VintedText.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaCompacta(producto: Producto, onDescartar: () -> Unit, onFavorito: () -> Unit, nombreBusqueda: String = "") {
    val info = plataformas[producto.plataforma]
    var mostrarChat by remember { mutableStateOf(false) }
    var botonActivo by remember { mutableStateOf<String?>(null) }

    if (mostrarChat) {
        Dialog(onDismissRequest = { mostrarChat = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    TextButton(
                        onClick = { mostrarChat = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Negociar precio m\u00e1s bajo", fontSize = 14.sp)
                    }
                    HorizontalDivider()
                    TextButton(
                        onClick = { mostrarChat = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Preguntar acerca del producto", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { botonActivo = null },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (info != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(info.colorFondo, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(producto.plataforma.first().toString(), fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(nombreBusqueda.ifBlank { producto.busqueda }, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), maxLines = 1)
            }
            Text(String.format("%.0f EUR", producto.precio), fontWeight = FontWeight.Bold,
                fontSize = 13.sp, color = if (MaterialTheme.colorScheme.background == BackgroundDark)
                    PrecioColorDark else PrecioColor, modifier = Modifier.padding(horizontal = 4.dp))

            // Guardar
            IconButton(
                onClick = { onFavorito(); botonActivo = null },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    if (producto.esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Guardar",
                    tint = if (producto.esFavorito) Favorito else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))

            // Chat
            IconButton(
                onClick = { botonActivo = null; mostrarChat = true },
                modifier = Modifier.size(22.dp)
            ) {
                Text("\u2026", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Secondary)
            }
            Spacer(modifier = Modifier.width(4.dp))

            // Descartar - doble click
            val descartarActivo = botonActivo == "descartar"
            IconButton(
                onClick = {
                    if (descartarActivo) { botonActivo = null; onDescartar() }
                    else { botonActivo = "descartar" }
                },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Descartar",
                    tint = if (descartarActivo) Color(0xFFE24B4A) else Favorito.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))

            // Comprar - doble click
            val comprarActivo = botonActivo == "comprar"
            IconButton(
                onClick = {
                    if (comprarActivo) { botonActivo = null }
                    else { botonActivo = "comprar" }
                },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Comprar",
                    tint = if (comprarActivo) Color(0xFF0F6E56) else VintedText.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

fun Modifier.textoVertical() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2)
        )
    }
}
