package com.example.testserver

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var mainContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainContent = findViewById(R.id.main_content)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Mostra una breve introduzione nella home (main_content)
        mainContent.text = "Benvenuto nella tua app WoT!\nUsa il menu in basso per navigare."

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    mainContent.text = "Benvenuto nella tua app WoT!\nUsa il menu in basso per navigare."
                    true
                }
                R.id.nav_sensors -> {
                    startActivity(Intent(this, SensorListActivity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    true
                }
                R.id.nav_server -> {
                    startActivity(Intent(this, StartServerActivity::class.java))
                    true
                }
                R.id.nav_media -> {
                    startActivity(Intent(this, PicAudioActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
