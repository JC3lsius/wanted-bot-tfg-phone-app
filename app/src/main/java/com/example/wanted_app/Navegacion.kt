package com.example.wanted_app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.lifecycle.Lifecycle
import com.example.wanted_app.ui.theme.WantedAppTheme

/**
 * Navega solo si la pantalla actual está RESUMED. Pulsar un botón muy rápido
 * disparaba varias navegaciones en el mismo frame (antes de que terminara la
 * transición), lo que apilaba pantallas duplicadas o, combinado con popBackStack,
 * dejaba el NavHost sin destino (pantalla en blanco). El estado RESUMED solo se
 * da en la pantalla "activa": tras la primera navegación deja de estarlo, así
 * que las pulsaciones extra se ignoran hasta que la transición termina.
 */
private fun NavController.navegarSeguro(
    ruta: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(ruta, builder)
    }
}

/**
 * Vuelve atrás solo si la pantalla actual está RESUMED, evitando que varias
 * pulsaciones rápidas hagan popBackStack de más y vacíen la pila.
 */
private fun NavController.volverSeguro() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

sealed class Pantalla(val ruta: String, val titulo: String, val icono: ImageVector) {
    object Inicio : Pantalla("inicio", "Inicio", Icons.Default.Home)
    object Productos : Pantalla("productos", "Productos", Icons.Default.ShoppingCart)
}

data class BotonBarra(val icono: ImageVector, val ruta: String)

@Composable
fun AppPrincipal() {
    WantedAppTheme {
        val navController = rememberNavController()
        val viewModel: ProductosViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

        // Ruta actual: la usamos para ocultar la barra inferior en login y registro.
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val rutaActual = navBackStackEntry?.destination?.route
        val mostrarBarra = rutaActual != "login" && rutaActual != "registro"

        // Auto-refresco GLOBAL: con sesión iniciada (fuera de login/registro), pide
        // la lista nueva cada Ajustes.intervaloRefrescoSeg segundos AUNQUE no estés en
        // la pantalla de productos, para que esté al día al volver. Se reinicia (con el
        // nuevo intervalo) al aplicar un cambio o al cambiar de sesión.
        val sesionActiva = rutaActual != null && rutaActual != "login" && rutaActual != "registro"
        LaunchedEffect(sesionActiva, Ajustes.intervaloRefrescoSeg) {
            if (sesionActiva) {
                while (true) {
                    delay(Ajustes.intervaloRefrescoSeg * 1000L)
                    viewModel.refrescarSilencioso()
                }
            }
        }

        val botonesBarra = listOf(
            BotonBarra(Icons.Default.Home, "inicio"),
            BotonBarra(Icons.Default.ShoppingCart, "productos"),
            BotonBarra(Icons.Default.Build, "config_bot"),
            BotonBarra(Icons.Default.Settings, "config_app"),
        )

        Scaffold(
            bottomBar = {
                // La barra inferior NO se muestra en login ni en registro.
                if (mostrarBarra) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            HorizontalDivider(thickness = 0.6.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                // Botones centrados en el medio, con separación uniforme.
                                // (Para repartirlos por todo el ancho: Arrangement.SpaceEvenly.)
                                horizontalArrangement = Arrangement.spacedBy(
                                    8.dp, Alignment.CenterHorizontally
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                botonesBarra.forEach { boton ->
                                    IconButton(onClick = { navController.navegarSeguro(boton.ruta) { launchSingleTop = true } }) {
                                        Icon(imageVector = boton.icono, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "login",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("login") {
                    PantallaLogin(
                        onLoginExitoso = {
                            navController.navegarSeguro(Pantalla.Inicio.ruta) {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onIrARegistro = { navController.navegarSeguro("registro") { launchSingleTop = true } }
                    )
                }
                composable("registro") {
                    PantallaRegistro(
                        onRegistroExitoso = {
                            // Tras registrarse, entra a la app y limpia login + registro de la pila.
                            navController.navegarSeguro(Pantalla.Inicio.ruta) {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onVolverALogin = { navController.volverSeguro() }
                    )
                }
                composable(Pantalla.Inicio.ruta) {
                    PantallaInicio(
                        onNavegar = { ruta ->
                            navController.navegarSeguro(ruta) {
                                popUpTo(Pantalla.Inicio.ruta) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Pantalla.Productos.ruta) { PantallaProductos(viewModel) }
                composable("config_bot") { PantallaConfigBot() }
                composable("config_app") {
                    PantallaConfigApp(
                        onCerrarSesion = {
                            Sesion.cerrar()
                            navController.navegarSeguro("login") {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}
