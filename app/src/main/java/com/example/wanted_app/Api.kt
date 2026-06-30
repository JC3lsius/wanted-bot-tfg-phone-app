package com.example.wanted_app

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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
    @SerializedName("busqueda_id") val busquedaId: Int? = null,
    val imagenes: List<String> = emptyList(),
    @SerializedName("fecha_detectado") val fechaDetectado: String? = null,
    val favorito: Boolean = false,
    val descartado: Boolean = false,
    val comprado: Boolean = false
)

// Respuesta de GET /stats
data class StatsDto(
    @SerializedName("total_productos") val totalProductos: Int = 0,
    @SerializedName("total_favoritos") val totalFavoritos: Int = 0,
    @SerializedName("total_comprados") val totalComprados: Int = 0,
    @SerializedName("total_busquedas") val totalBusquedas: Int = 0
)

// Auth
data class LoginRequest(val email: String, val password: String)
data class DispositivoRequest(@SerializedName("token_fcm") val tokenFcm: String)
data class RegistroRequest(val email: String, val nombre: String, val password: String)
data class TokenResponse(val token: String, val nombre: String, val email: String)

// Respuesta genérica
data class MensajeResponse(val mensaje: String, val exito: Boolean = true)

// Búsqueda (respuesta de GET/POST /busquedas)
data class BusquedaDto(
    val id: Int,
    val nombre: String,
    val plataformas: List<String> = emptyList(),
    @SerializedName("palabras_incluidas") val palabrasIncluidas: List<String> = emptyList(),
    @SerializedName("palabras_excluidas") val palabrasExcluidas: List<String> = emptyList(),
    @SerializedName("precio_min") val precioMin: Double = 0.0,
    @SerializedName("precio_max") val precioMax: Double = 0.0,
    @SerializedName("estado_articulo") val estadoArticulo: String = "",
    val ubicacion: String = "",
    val categoria: String = "",
    val tipo: String = "listener",
    val profundidad: Int = 1,
    @SerializedName("limite_productos") val limiteProductos: Int = 0,
    @SerializedName("omitir_primera") val omitirPrimera: Boolean = true,
    val activa: Boolean = false,
    @SerializedName("estado_hilo") val estadoHilo: String = "detenido",
    @SerializedName("num_productos") val numProductos: Int = 0
)

// Cuerpo de POST /busquedas
data class BusquedaCrearRequest(
    val nombre: String,
    val plataformas: List<String> = emptyList(),
    @SerializedName("palabras_incluidas") val palabrasIncluidas: List<String> = emptyList(),
    @SerializedName("palabras_excluidas") val palabrasExcluidas: List<String> = emptyList(),
    @SerializedName("precio_min") val precioMin: Double = 0.0,
    @SerializedName("precio_max") val precioMax: Double = 99999.0,
    @SerializedName("estado_articulo") val estadoArticulo: String = "",
    val ubicacion: String = "",
    val categoria: String = "",
    val tipo: String = "listener",
    val profundidad: Int = 1,
    @SerializedName("limite_productos") val limiteProductos: Int = 0,
    @SerializedName("omitir_primera") val omitirPrimera: Boolean = true
)

// =====================================================================
//  Endpoints
// =====================================================================

interface ApiService {

    @GET("productos")
    suspend fun getProductos(
        @Query("plataforma") plataforma: String? = null,
        @Query("busqueda") busqueda: String? = null,
        @Query("solo_favoritos") soloFavoritos: Boolean = false,
        @Query("pagina") pagina: Int = 1,
        @Query("limite") limite: Int = 50
    ): List<ProductoDto>

    @GET("stats")
    suspend fun getStats(): StatsDto

    @PUT("productos/{id}/favorito")
    suspend fun toggleFavorito(@Path("id") id: String): MensajeResponse

    @PUT("productos/{id}/descartar")
    suspend fun descartar(@Path("id") id: String): MensajeResponse

    @POST("auth/login")
    suspend fun login(@Body datos: LoginRequest): TokenResponse

    @POST("auth/registro")
    suspend fun registro(@Body datos: RegistroRequest): TokenResponse

    @POST("dispositivos")
    suspend fun registrarDispositivo(@Body datos: DispositivoRequest): MensajeResponse

    // --- Búsquedas (Configuración del Bot) ---

    @GET("busquedas")
    suspend fun getBusquedas(): List<BusquedaDto>

    @POST("busquedas")
    suspend fun crearBusqueda(@Body datos: BusquedaCrearRequest): BusquedaDto

    @PUT("busquedas/{id}")
    suspend fun actualizarBusqueda(@Path("id") id: Int, @Body datos: BusquedaCrearRequest): BusquedaDto

    @POST("busquedas/{id}/iniciar")
    suspend fun iniciarBusqueda(@Path("id") id: Int): MensajeResponse

    @POST("busquedas/{id}/parar")
    suspend fun pararBusqueda(@Path("id") id: Int): MensajeResponse

    @DELETE("busquedas/{id}")
    suspend fun borrarBusqueda(@Path("id") id: Int): MensajeResponse

    @DELETE("productos")
    suspend fun borrarProductos(
        @Query("busqueda") busqueda: String? = null,
        @Query("busqueda_id") busquedaId: Int? = null
    ): MensajeResponse
}

// =====================================================================
//  Sesión en memoria — guarda el token tras iniciar sesión
// =====================================================================

object Sesion {
    var token: String? = null
    var nombre: String? = null
    var email: String? = null

    fun guardar(t: TokenResponse) {
        token = t.token
        nombre = t.nombre
        email = t.email
    }

    fun cerrar() {
        token = null
        nombre = null
        email = null
    }
}

// =====================================================================
//  Cliente Retrofit (con timeouts para que NUNCA se quede colgado)
// =====================================================================

object RetrofitCliente {

    // 10.0.2.2 = el "localhost de tu PC" visto desde el EMULADOR de Android.
    // En un MÓVIL FÍSICO, cambia esto por la IP de tu PC en la red local,
    // por ejemplo: "http://192.168.1.40:8000/"  (y el móvil en el mismo Wi-Fi).
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        // Adjunta el token JWT (si hay sesion iniciada) a TODAS las peticiones.
        .addInterceptor { chain ->
            val original = chain.request()
            val peticion = Sesion.token?.let { token ->
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } ?: original
            chain.proceed(peticion)
        }
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
