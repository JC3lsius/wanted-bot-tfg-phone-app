package com.example.wanted_app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Ajustes de la app en memoria (igual que [Sesion]). De momento solo guarda el
 * intervalo de auto-refresco de la lista de productos, configurable entre
 * [INTERVALO_MIN] y [INTERVALO_MAX] segundos desde la pantalla de Configuración.
 *
 * Al ser en memoria, vuelve al valor por defecto cada vez que se abre la app.
 * (Para que persista entre arranques habría que respaldarlo en SharedPreferences.)
 */
object Ajustes {
    const val INTERVALO_MIN = 1
    const val INTERVALO_MAX = 40
    private const val INTERVALO_POR_DEFECTO = 15

    /** Segundos entre refrescos automáticos de la pantalla de productos. */
    var intervaloRefrescoSeg by mutableStateOf(INTERVALO_POR_DEFECTO)
        private set

    /** Fija el intervalo, acotándolo siempre al rango permitido. */
    fun fijarIntervalo(segundos: Int) {
        intervaloRefrescoSeg = segundos.coerceIn(INTERVALO_MIN, INTERVALO_MAX)
    }
}
