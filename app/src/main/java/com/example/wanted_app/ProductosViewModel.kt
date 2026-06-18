package com.example.wanted_app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProductosViewModel : ViewModel() {

    // Lista observable por Compose. Ahora se llena desde el backend (ya no es mock).
    private val _productos = mutableStateListOf<Producto>()
    val productos: List<Producto> get() = _productos

    val favoritos: List<Producto> get() = _productos.filter { it.esFavorito }

    // Estado opcional por si quieres mostrar "cargando" o un error en la UI.
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        cargarProductos()
    }

    /** Pide los productos al backend (GET /productos) y rellena la lista. */
    fun cargarProductos() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                val lista = RetrofitCliente.api.getProductos(limite = 200)
                _productos.clear()
                _productos.addAll(lista.map { it.aProducto() })
            } catch (e: Exception) {
                error = "No se pudo conectar con el servidor: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    fun toggleFavorito(id: String) {
        val index = _productos.indexOfFirst { it.id == id }
        if (index != -1) {
            // Cambio local inmediato (la UI responde al momento).
            _productos[index] = _productos[index].copy(esFavorito = !_productos[index].esFavorito)
        }
        // Persistir en el backend (best-effort: si falla, no rompe la UI).
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
    imagenes = imagenes
)
