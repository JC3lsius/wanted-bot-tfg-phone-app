package com.example.wanted_app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

/**
 * Abre el anuncio de un producto en la APP de su plataforma (Vinted, Wallapop,
 * Milanuncios o eBay) si está instalada; si no, lo abre en el NAVEGADOR.
 *
 * Flujo:
 *   1. Mapea el nombre de plataforma del backend ("Vinted", "Wallapop"…) al
 *      package de la app oficial en Android.
 *   2. Si esa app está instalada, lanza un ACTION_VIEW con setPackage(...) para
 *      que Android abra el enlace DENTRO de ella. Si la app tiene declarado su
 *      dominio (App Links), cae directamente en la ficha del producto.
 *   3. Si la app no está instalada (o está pero no sabe abrir ese enlace
 *      concreto), hace fallback al navegador con la misma URL.
 *
 * IMPORTANTE (Android 11+): para que la comprobación "¿está instalada?" funcione
 * hay que declarar los <queries> de estos packages en AndroidManifest.xml. Sin
 * eso, getPackageInfo() lanza NameNotFoundException aunque la app SÍ esté
 * instalada, y siempre acabaríamos en el navegador.
 */
fun abrirProductoEnPlataforma(context: Context, enlace: String, plataforma: String) {
    if (enlace.isBlank()) {
        Toast.makeText(context, "Este producto no tiene enlace.", Toast.LENGTH_SHORT).show()
        return
    }

    val uri = Uri.parse(enlace)
    val paquete = paqueteDePlataforma(plataforma)

    // 1) Si la app de la plataforma está instalada, intentamos abrir el enlace EN ella.
    if (paquete != null && appInstalada(context, paquete)) {
        val intentApp = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(paquete)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intentApp)
            return
        } catch (_: ActivityNotFoundException) {
            // La app está instalada pero no maneja este enlace concreto.
            // Continuamos al paso 2 (navegador).
        }
    }

    // 2) Fallback: abrir en el navegador / manejador por defecto del sistema.
    val intentWeb = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intentWeb)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No se pudo abrir el enlace.", Toast.LENGTH_SHORT).show()
    }
}

/** ¿Hay una app instalada con este package? */
private fun appInstalada(context: Context, paquete: String): Boolean = try {
    context.packageManager.getPackageInfo(paquete, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

/**
 * Package de la app oficial de cada plataforma en Android.
 *
 * VERIFÍCALOS en Google Play: abre la ficha de cada app en el navegador y mira
 * el parámetro id de la URL ->  play.google.com/store/apps/details?id=<PACKAGE>
 * El de Milanuncios es el más propenso a no coincidir con esto.
 */
private fun paqueteDePlataforma(plataforma: String): String? =
    when (plataforma.trim().lowercase()) {
        "vinted"      -> "fr.vinted"
        "wallapop"    -> "com.wallapop"
        "milanuncios" -> "com.milanuncios"
        "ebay"        -> "com.ebay.mobile"
        else          -> null
    }
