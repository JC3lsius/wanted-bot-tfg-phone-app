package com.example.wanted_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MiFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val titulo = message.data["nombre"] ?: message.notification?.title ?: "Nuevo producto"
        val cuerpo = message.data["precio"] ?: message.notification?.body ?: ""
        // Leemos el producto_id AQUÍ, que es donde existe 'message':
        val productoId = message.data["producto_id"] ?: System.currentTimeMillis().toString()

        mostrarNotificacion(titulo, cuerpo, productoId)
    }

    // Registra el token también cuando FCM lo rota (no solo al arrancar).
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try { RetrofitCliente.api.registrarDispositivo(DispositivoRequest(token)) } catch (_: Exception) {}
        }
    }

    private fun mostrarNotificacion(titulo: String, cuerpo: String, productoId: String) {
        val canalId = "productos_nuevos"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val canal = NotificationChannel(
            canalId,
            "Productos nuevos",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(canal)

        val notificacion = NotificationCompat.Builder(this, canalId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // ID único y estable por producto → sin colisiones:
        manager.notify(productoId.hashCode(), notificacion)
    }
}