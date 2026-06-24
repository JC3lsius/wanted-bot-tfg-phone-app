package com.example.wanted_app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProductosViewModel : ViewModel() {

    private val _productos = mutableStateListOf<Producto>()
    val productos: List<Producto> get() = _productos

    val favoritos: List<Producto> get() = _productos.filter { it.esFavorito }

    var cargando by mutableStateOf(false)        // carga inicial / refresco (página 1)
        private set
    var cargandoMas by mutableStateOf(false)     // cargando la siguiente página
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var hayMas by mutableStateOf(true)           // ¿quedan más páginas por cargar?
        private set

    private var pagina = 1
    private val tamPagina = 30

    init {
        cargarProductos()
    }

    /** Carga la primera página y resetea la lista (al entrar a la pantalla o al refrescar). */
    fun cargarProductos() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                val lista = RetrofitCliente.api.getProductos(pagina = 1, limite = tamPagina)
                _productos.clear()
                _productos.addAll(lista.map { it.aProducto() })
                pagina = 1
                hayMas = lista.size == tamPagina
            } catch (e: Exception) {
                error = "No se pudo conectar con el servidor: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    /** Carga la siguiente página y la añade al final (scroll infinito). */
    fun cargarMas() {
        if (cargandoMas || cargando || !hayMas) return
        viewModelScope.launch {
            cargandoMas = true
            try {
                val siguiente = pagina + 1
                val lista = RetrofitCliente.api.getProductos(pagina = siguiente, limite = tamPagina)
                // Si llegaron productos nuevos mientras paginabas, el offset se desplaza y
                // algunos podrían repetirse: filtramos por id los que ya tenemos.
                val nuevos = lista.map { it.aProducto() }
                    .filter { nuevo -> _productos.none { it.id == nuevo.id } }
                _productos.addAll(nuevos)
                pagina = siguiente
                hayMas = lista.size == tamPagina
            } catch (e: Exception) {
                error = "No se pudieron cargar más productos: ${e.message}"
            } finally {
                cargandoMas = false
            }
        }
    }

    fun toggleFavorito(id: String) {
        val index = _productos.indexOfFirst { it.id == id }
        if (index != -1) {
            _productos[index] = _productos[index].copy(esFavorito = !_productos[index].esFavorito)
        }
        viewModelScope.launch {
            try { RetrofitCliente.api.toggleFavorito(id) } catch (_: Exception) {}
        }
    }

    fun descartar(id: String) {
        _productos.removeAll { it.id == id }
        viewModelScope.launch {
            try { RetrofitCliente.api.descartar(id) } catch (_: Exception) {}
        }
    }

    /** Borra TODOS los productos del backend y vacía la lista local. */
    fun borrarTodos() {
        viewModelScope.launch {
            try {
                RetrofitCliente.api.borrarProductos()
                _productos.clear()
                hayMas = false
            } catch (e: Exception) {
                error = "No se pudieron borrar los productos: ${e.message}"
            }
        }
    }
}

/** Convierte el DTO de red al modelo Producto que usa la app. */
private fun ProductoDto.aProducto() = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    plataforma = plataforma,
    imagenUrl = imagenUrl,
    enlace = enlace,
    tiempoDetectado = fechaDetectado ?: "",
    esFavorito = favorito,
    busqueda = busqueda,
    busquedaId = busquedaId,
    imagenes = imagenes
)
