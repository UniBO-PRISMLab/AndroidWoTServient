package com.example.testserver

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Stato connessione
        val connectionStatus = findViewById<TextView>(R.id.connectionStatus)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Bottoni
        val sensorListButton: Button = findViewById(R.id.sensorListButton)
        val sensorDataButton: Button = findViewById(R.id.sensorDataButton)
        sensorDataButton.isEnabled = false
        val repeaterButton: Button = findViewById(R.id.repeaterButton)
        val picButton: Button = findViewById(R.id.picAudioButton)
        val shareSensorsButton: Button = findViewById(R.id.shareSensorsButton)

        // Prima di avviare foreground service devo chiedere permesso per notifica
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        // Avvia foreground service
        val serviceIntent = Intent(this, WoTService::class.java)
        startForegroundService(serviceIntent)

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                connectionStatus.text = "In connessione..."
                progressBar.visibility = View.VISIBLE
            }

            // Fingi attesa connessione?
            delay(1500)
            withContext(Dispatchers.Main) {
                connectionStatus.text = "Connesso!"
                progressBar.visibility = View.GONE
                sensorDataButton.isEnabled = true
            }
        }

        sensorListButton.setOnClickListener {
            startActivity(Intent(this, SensorListActivity::class.java))
        }

        sensorDataButton.setOnClickListener {
            startActivity(Intent(this, SensorDataActivity::class.java))
        }

        picButton.setOnClickListener {
            startActivity(Intent(this, PicAudioActivity::class.java))
        }

        shareSensorsButton.setOnClickListener {
            startActivity(Intent(this, ShareSensorsActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Pulizia delle Coroutine
    }
}