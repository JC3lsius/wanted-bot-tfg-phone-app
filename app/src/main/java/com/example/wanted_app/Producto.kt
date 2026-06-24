package com.example.wanted_app

data class Producto(
    val id: String,
    val nombre: String,
    val precio: Double,
    val plataforma: String,
    val imagenUrl: String = "",
    val enlace: String = "",
    val tiempoDetectado: String = "",
    val esFavorito: Boolean = false,
    val busqueda: String = "",
    val busquedaId: Int? = null,
    val imagenes: List<String> = emptyList()
)