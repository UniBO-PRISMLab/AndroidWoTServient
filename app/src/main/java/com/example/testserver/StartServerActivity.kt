package com.example.testserver

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StartServerActivity : AppCompatActivity() {
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_server)
        startButton = findViewById(R.id.startBtn)

        startButton.setOnClickListener {
            // Richiesta permesso notifiche se necessario
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
                } else {
                    startWoTService()
                }
            } else {
                startWoTService()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Il permesso è stato concesso, avvia il servizio
            startWoTService()
        } else {
            // Permesso non concesso, messaggio di errore!
            Toast.makeText(this, "Permesso negato, impossibile avviare il server!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startWoTService() {
        startForegroundService(Intent(this, WoTService::class.java))
        Toast.makeText(this, "Server Avviato con Successo!", Toast.LENGTH_SHORT).show()
    }
}