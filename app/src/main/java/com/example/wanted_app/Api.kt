package com.example.wanted_app

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// =====================================================================
//  DTOs — lo que viaja por la red (nombres EXACTOS del JSON del backend)
// =====================================================================

// Respuesta de GET /productos
data class ProductoDto(
    val id: String,
    val nombre: String,
    val precio: Double,
    val plataforma: String,
    @SerializedName("imagen_url") val imagenUrl: String = "",
    val enlace: String = "",
    val busqueda: String = "",
    @SerializedName("busqueda_id") val busquedaId: Int = 0,
    val imagenes: List<String> = emptyList(),
    @SerializedName("fecha_detectado") val fechaDetectado: String? = null,
    val favorito: Boolean = false,
    val descartado: Boolean = false,
    val comprado: Boolean = false
)

// Auth
data class LoginRequest(val email: String, val password: String)
data class RegistroRequest(val email: String, val nombre: String, val password: String)
data class TokenResponse(val token: String, val nombre: String, val email: String)

// Respuesta genérica
data class MensajeResponse(val mensaje: String, val exito: Boolean = true)

// =====================================================================
//  Endpoints
// =====================================================================

interface ApiService {

    @GET("productos")
    suspend fun getProductos(
        @Query("plataforma") plataforma: String? = null,
        @Query("busqueda") busqueda: String? = null,
        @Query("solo_favoritos") soloFavoritos: Boolean = false,
        @Query("limite") limite: Int = 50
    ): List<ProductoDto>

    @PUT("productos/{id}/favorito")
    suspend fun toggleFavorito(@Path("id") id: String): MensajeResponse

    @PUT("productos/{id}/descartar")
    suspend fun descartar(@Path("id") id: String): MensajeResponse

    @POST("auth/login")
    suspend fun login(@Body datos: LoginRequest): TokenResponse

    @POST("auth/registro")
    suspend fun registro(@Body datos: RegistroRequest): TokenResponse
}

// =====================================================================
//  Cliente Retrofit
// =====================================================================

object RetrofitCliente {

    // 10.0.2.2 = el "localhost de tu PC" visto desde el EMULADOR de Android.
    // En un MÓVIL FÍSICO, cambia esto por la IP de tu PC en la red local,
    // por ejemplo: "http://192.168.1.40:8000/"  (y el móvil en el mismo Wi-Fi).
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
