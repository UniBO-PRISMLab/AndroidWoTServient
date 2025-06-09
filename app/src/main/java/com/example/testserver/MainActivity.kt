package com.example.testserver

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bottoni
        val sensorListButton: Button = findViewById(R.id.sensorListButton)
        val sensorDataButton: Button = findViewById(R.id.sensorDataButton)
        val repeaterButton: Button = findViewById(R.id.repeaterButton)
        val picButton: Button = findViewById(R.id.picAudioButton)
        val shareSensorsButton: Button = findViewById(R.id.shareSensorsButton)
        val settingsButton: Button = findViewById(R.id.settingsButton)
        val statsButton: Button = findViewById(R.id.statsButton)
        val startServerButton: Button = findViewById(R.id.startServerBtn)

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
            startActivity(Intent(this, SelectSensorsActivity::class.java))
        }

        statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        startServerButton.setOnClickListener {
            startActivity(Intent(this, StartServerActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Pulizia delle Coroutine
    }
}