package com.example.testserver

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.binding.http.HttpProtocolServer

class WoTService : Service() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var servient: Servient
    private lateinit var wot: Wot

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        startWoTServer()
    }

    private fun startWoTServer() {
        coroutineScope.launch {
            try {
                servient = Servient(
                    servers = listOf(HttpProtocolServer()),
                    clientFactories = listOf(HttpProtocolClientFactory())
                )
                wot = Wot.create(servient)
                WoTClientHolder.wot = wot
                servient.start()

                val server = Server(wot, servient, applicationContext)
                server.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "wot_service_channel"
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WoT Server",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, WoTService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Server WoT attivo")
            .setContentText("Il server è in esecuzione")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .addAction(R.drawable.ic_launcher_background, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action == "STOP_SERVICE") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }
}