package com.example.wanted_app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.wanted_app.ui.theme.WantedAppTheme

sealed class Pantalla(val ruta: String, val titulo: String, val icono: ImageVector) {
    object Inicio : Pantalla("inicio", "Inicio", Icons.Default.Home)
    object Busquedas : Pantalla("busquedas", "Búsquedas", Icons.Default.Search)
    object Productos : Pantalla("productos", "Productos", Icons.Default.ShoppingCart)
    object Favoritos : Pantalla("favoritos", "Favoritos", Icons.Default.Favorite)
    object Perfil : Pantalla("perfil", "Perfil", Icons.Default.Person)
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

        val botonesBarra = listOf(
            BotonBarra(Icons.Default.Home, "inicio"),
            BotonBarra(Icons.Default.Person, "perfil"),
            BotonBarra(Icons.Default.Settings, "config_app"),
            BotonBarra(Icons.Default.Build, "config_bot"),
            BotonBarra(Icons.Default.Favorite, "favoritos"),
            BotonBarra(Icons.Default.ShoppingCart, "productos"),
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
                                    IconButton(onClick = { navController.navigate(boton.ruta) }) {
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
                            navController.navigate(Pantalla.Inicio.ruta) {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onIrARegistro = { navController.navigate("registro") }
                    )
                }
                composable("registro") {
                    PantallaRegistro(
                        onRegistroExitoso = {
                            // Tras registrarse, entra a la app y limpia login + registro de la pila.
                            navController.navigate(Pantalla.Inicio.ruta) {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onVolverALogin = { navController.popBackStack() }
                    )
                }
                composable(Pantalla.Inicio.ruta) {
                    PantallaInicio(
                        onNavegar = { ruta ->
                            navController.navigate(ruta) {
                                popUpTo(Pantalla.Inicio.ruta) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Pantalla.Busquedas.ruta) { PantallaBusquedas() }
                composable(Pantalla.Productos.ruta) { PantallaProductos(viewModel) }
                composable(Pantalla.Favoritos.ruta) { PantallaFavoritos(viewModel) }
                composable(Pantalla.Perfil.ruta) { PantallaPerfil() }
                composable("config_bot") { PantallaConfigBot() }
                composable("config_app") { PantallaConfigApp() }
            }
        }
    }
}
