package com.example.testserver

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {
    companion object {
        var serverRunning: Boolean = false
    }

    protected fun setupBottomNavigation(selectedItemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (selectedItemId != R.id.nav_home) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(intent)
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (selectedItemId != R.id.nav_settings) {
                        val intent = Intent(this, SelectSensorsActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(intent)
                    }
                    true
                }
                R.id.nav_stats -> {
                    if (selectedItemId != R.id.nav_stats) {
                        val intent = Intent(this, StatsActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(intent)
                    }
                    true
                }
                R.id.nav_media -> {
                    if (selectedItemId != R.id.nav_media) {
                        if (!serverRunning) {
                            Toast.makeText(this, "Avvia il Server prima!", Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            val intent = Intent(this, PicAudioActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            startActivity(intent)
                            true
                        }
                    } else {
                        true
                    }
                }
                else -> false
            }
        }
    }
}