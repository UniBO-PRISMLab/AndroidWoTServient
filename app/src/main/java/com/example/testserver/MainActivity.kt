package com.example.testserver

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity() {

    private lateinit var mainContentText: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_with_nav)
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
        val homeLayout = layoutInflater.inflate(R.layout.activity_main, contentFrame, false)
        contentFrame.addView(homeLayout)

        mainContentText = homeLayout.findViewById(R.id.main_content)
        startButton = homeLayout.findViewById(R.id.startBtn)
        setupBottomNavigation(R.id.nav_home)

        // Mostra una breve introduzione nella home (main_content)
        mainContentText.text = "Benvenuto nella tua app WoT!\nUsa il menu in basso per navigare."
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
        serverRunning = true
    }
}
