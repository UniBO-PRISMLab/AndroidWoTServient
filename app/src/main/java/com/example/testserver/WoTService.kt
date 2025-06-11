package com.example.testserver

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
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
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WoTService : Service() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var servient: Servient? = null
    private var wot: Wot? = null
    private var server: Server? = null

    // Mutex per evitare race condition?
    private val serverMutex = Mutex()
    private var isServerRunning = false

    private val preferenceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "PREFERENCES_UPDATED") {
                val updatedType = intent.getStringExtra("update_type") ?: ""
                coroutineScope.launch {
                    serverMutex.withLock {
                        when (updatedType) {
                            "port" -> {
                                // Cambio porta --> stop e riavvio
                                stopWoTServerInternal()
                                Log.d("SERVER", "Riavvio Server")
                                delay(1000)
                                startWoTServerInternal()
                                Log.d("SERVER", "Server riattivo!")
                            }
                            "sensors" -> {
                                // Aggiunta/rimozione sensori --> niente stop
                                server?.updateExposedThings()
                                Log.d("SERVER", "Server aggiornato!")
                            }
                            else -> {
                                // Default
                                server?.updateExposedThings()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        // Carico le stats
        ServientStatsPrefs.load(applicationContext)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_NOT_EXPORTED else 0
        registerReceiver(preferenceReceiver, IntentFilter("PREFERENCES_UPDATED"), flags)
        coroutineScope.launch {
            startWoTServer()
        }
    }

    private suspend fun startWoTServer() {
        serverMutex.withLock {
            startWoTServerInternal()
        }
    }

    private suspend fun startWoTServerInternal() {
        try {
            if (isServerRunning) {
                Log.d("SERVER", "Server già in esecuzione!")
                return
            }

            stopWoTServerInternal()
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val port = prefs.getString("server_port", "8080")?.toIntOrNull() ?: 8080
            Log.d("SERVER", "Avvio Server sulla porta $port")

            servient = Servient(
                servers = listOf(HttpProtocolServer(bindPort = port)),
                clientFactories = listOf(HttpProtocolClientFactory())
            )

            wot = Wot.create(servient!!)
            WoTClientHolder.wot = wot
            servient!!.start()

            server = Server(wot!!, servient!!, applicationContext)
            server!!.start()

            isServerRunning = true
            prefs.edit().putBoolean("server_started", true).apply()
            Log.d("SERVER", "Server avviato con successo sulla porta $port")
        } catch (e: Exception) {
            Log.e("WOT_SERVICE", "Errore durante avvio server: ", e)
            stopWoTServerInternal()
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
        try {
            unregisterReceiver(preferenceReceiver)
        } catch (e: Exception) {
            Log.e("WOT_SERVICE", "Errore durante unregister receiver: ", e)
        }
        coroutineScope.launch {
            stopWoTServer()
            // Salvo stats
            ServientStatsPrefs.save(applicationContext)
            coroutineScope.cancel()
        }
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

    private suspend fun stopWoTServer() {
        serverMutex.withLock {
            stopWoTServerInternal()
        }
    }

    private suspend fun stopWoTServerInternal() {
        try {
            if (!isServerRunning) {
                Log.d("SERVER", "Server già fermo, skip..")
                return
            }
            Log.d("SERVER", "Fermando Server..")
            server?.let {
                try {
                    it.stop()
                    Log.d("SERVER", "Server fermo")
                } catch (e: Exception) {
                    Log.e("SERVER", "Errore durante stop server: ", e)
                }
            }
            server = null

            servient?.let {
                try {
                    it.shutdown()
                    Log.d("SERVER", "Servient fermato")
                } catch (e: Exception) {
                    Log.e("SERVER", "Errore durante shutdown servient: ", e)
                }
            }
            servient = null
            wot = null
            isServerRunning = false
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            prefs.edit{putBoolean("server_started", false)}
            Log.d("SERVER", "Stop completo!")
            BaseActivity.serverRunning = false
        } catch (e: Exception) {
            Log.e("WOT_SERVICE", "Errore durante stop server: ", e)
        }
    }

}