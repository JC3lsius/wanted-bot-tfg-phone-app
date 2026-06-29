package com.example.wanted_app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// --- API falsa: devuelve las páginas configuradas y registra las llamadas ---
private class FakeApiService(
    var paginas: Map<Int, List<ProductoDto>> = emptyMap(),
    var lanzarError: Boolean = false
) : ApiService {

    val favoritosLlamados = mutableListOf<String>()
    val descartadosLlamados = mutableListOf<String>()
    var borrarProductosLlamado = false
    val paginasPedidas = mutableListOf<Int>()

    override suspend fun getProductos(
        plataforma: String?, busqueda: String?, soloFavoritos: Boolean, pagina: Int, limite: Int
    ): List<ProductoDto> {
        if (lanzarError) throw RuntimeException("fallo de red simulado")
        paginasPedidas.add(pagina)
        return paginas[pagina] ?: emptyList()
    }

    override suspend fun toggleFavorito(id: String): MensajeResponse {
        favoritosLlamados.add(id); return MensajeResponse("ok")
    }
    override suspend fun descartar(id: String): MensajeResponse {
        descartadosLlamados.add(id); return MensajeResponse("ok")
    }
    override suspend fun borrarProductos(busqueda: String?, busquedaId: Int?): MensajeResponse {
        borrarProductosLlamado = true; return MensajeResponse("ok")
    }

    // Resto de métodos: no se usan en estos tests, devuelven valores triviales.
    override suspend fun getStats() = StatsDto()
    override suspend fun login(datos: LoginRequest) = TokenResponse("", "", "")
    override suspend fun registro(datos: RegistroRequest) = TokenResponse("", "", "")
    override suspend fun getBusquedas() = emptyList<BusquedaDto>()
    override suspend fun crearBusqueda(datos: BusquedaCrearRequest) = BusquedaDto(id = 0, nombre = "")
    override suspend fun actualizarBusqueda(id: Int, datos: BusquedaCrearRequest) = BusquedaDto(id = 0, nombre = "")
    override suspend fun iniciarBusqueda(id: Int) = MensajeResponse("ok")
    override suspend fun pararBusqueda(id: Int) = MensajeResponse("ok")
    override suspend fun borrarBusqueda(id: Int) = MensajeResponse("ok")
    override suspend fun registrarDispositivo(datos: DispositivoRequest) = MensajeResponse("ok")
}

private fun dto(id: String, fav: Boolean = false) =
    ProductoDto(id = id, nombre = "P$id", precio = 1.0, plataforma = "Vinted", favorito = fav)

@OptIn(ExperimentalCoroutinesApi::class)
class ProductosViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cargarProductos_rellenaLista_calculaHayMas_yTerminaDeCargar() {
        runTest {
            val fake = FakeApiService(mapOf(1 to listOf(dto("a"), dto("b"), dto("c"))))
            val vm = ProductosViewModel(fake)

            vm.cargarProductos()

            assertEquals(3, vm.productos.size)
            assertFalse(vm.hayMas)     // 3 < 30 → no quedan más páginas
            assertFalse(vm.cargando)   // el finally restauró 'cargando'
            assertNull(vm.error)
        }
    }

    @Test
    fun refrescarSilencioso_aniadeSoloLosNuevosAlPrincipio() {
        runTest {
            val fake = FakeApiService(mapOf(1 to listOf(dto("a"), dto("b"))))
            val vm = ProductosViewModel(fake)
            vm.cargarProductos()                       // lista = [a, b]

            fake.paginas = mapOf(1 to listOf(dto("c"), dto("a")))  // c nuevo, a repetido
            val aniadidos = vm.refrescarSilencioso()

            assertEquals(1, aniadidos)                 // solo 'c' es nuevo
            assertEquals(listOf("c", "a", "b"), vm.productos.map { it.id })  // 'c' va delante
        }
    }

    @Test
    fun cargarMas_aniadeLaSiguientePaginaAlFinal_yAvanzaPaginacion() {
        runTest {
            val fake = FakeApiService(mapOf(
                1 to List(30) { dto("p$it") },         // página 1 llena → hayMas = true
                2 to listOf(dto("q1"), dto("q2"))      // página 2 con resultados
            ))
            val vm = ProductosViewModel(fake)
            vm.cargarProductos()

            vm.cargarMas()

            assertEquals(32, vm.productos.size)                       // 30 + 2
            assertEquals(listOf("q1", "q2"), vm.productos.takeLast(2).map { it.id })  // al final
            assertTrue(2 in fake.paginasPedidas)                     // pidió la página 2
        }
    }

    @Test
    fun cargarMas_filtraDuplicadosPorId() {
        runTest {
            val fake = FakeApiService(mapOf(
                1 to List(30) { dto("p$it") },
                2 to listOf(dto("p29"), dto("nuevo"))  // un repetido + uno nuevo
            ))
            val vm = ProductosViewModel(fake)
            vm.cargarProductos()
            vm.cargarMas()

            assertEquals(31, vm.productos.size)                      // 30 + 1 (no 32)
            assertEquals(1, vm.productos.count { it.id == "p29" })   // p29 no se duplica
            assertTrue(vm.productos.any { it.id == "nuevo" })
        }
    }

    @Test
    fun cargarMas_noPideMasPaginas_siNoQuedan() {
        runTest {
            val fake = FakeApiService(mapOf(
                1 to listOf(dto("a"), dto("b")),   // 2 < 30 → hayMas = false
                2 to listOf(dto("c"))              // existe, pero NO debe pedirse
            ))
            val vm = ProductosViewModel(fake)
            vm.cargarProductos()
            assertFalse(vm.hayMas)

            vm.cargarMas()

            assertEquals(listOf("a", "b"), vm.productos.map { it.id })  // lista sin cambios
            assertFalse(2 in fake.paginasPedidas)                      // nunca pidió la página 2
        }
    }

    @Test
    fun toggleFavorito_invierteElFlagLocal_yLlamaALaApi() {
        runTest {
            val fake = FakeApiService(mapOf(1 to listOf(dto("a", fav = false))))
            val vm = ProductosViewModel(fake)
            vm.cargarProductos()

            assertFalse(vm.productos.first { it.id == "a" }.esFavorito)
            vm.toggleFavorito("a")
            assertTrue(vm.productos.first { it.id == "a" }.esFavorito)  // cambio optimista local
            assertTrue("a" in fake.favoritosLlamados)                  // y se llamó a la API
        }
    }

    @Test
    fun descartar_eliminaElProductoDeLaLista_yLlamaALaApi() {
        runTest {
            val fake = FakeApiService(mapOf(1 to listOf(dto("a"), dto("b"))))
            val vm = ProductosViewModel(fake)
            vm.cargarProductos()

            vm.descartar("a")

            assertEquals(listOf("b"), vm.productos.map { it.id })  // 'a' eliminado
            assertTrue("a" in fake.descartadosLlamados)            // y se llamó a la API
        }
    }

    @Test
    fun borrarTodos_vaciaLaLista_yLlamaALaApi() {
        runTest {
            val fake = FakeApiService(mapOf(1 to listOf(dto("a"), dto("b"), dto("c"))))
            val vm = ProductosViewModel(fake)
            vm.cargarProductos()
            assertEquals(3, vm.productos.size)

            vm.borrarTodos()

            assertTrue(vm.productos.isEmpty())
            assertFalse(vm.hayMas)
            assertTrue(fake.borrarProductosLlamado)
        }
    }

    @Test
    fun cargarProductos_siLaApiFalla_guardaError_yNoSeQuedaCargando() {
        runTest {
            val fake = FakeApiService(lanzarError = true)
            val vm = ProductosViewModel(fake)

            vm.cargarProductos()

            assertNotNull(vm.error)        // se guardó un mensaje de error
            assertFalse(vm.cargando)       // el finally restauró 'cargando'
            assertTrue(vm.productos.isEmpty())
        }
    }
}