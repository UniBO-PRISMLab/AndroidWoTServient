package com.example.testserver

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNavigation(selectedItemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (selectedItemId != R.id.nav_home)
                        startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_sensors -> {
                    if (selectedItemId != R.id.nav_sensors)
                        startActivity(Intent(this, SensorListActivity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    if (selectedItemId != R.id.nav_stats)
                        startActivity(Intent(this, StatsActivity::class.java))
                    true
                }
                R.id.nav_server -> {
                    if (selectedItemId != R.id.nav_server)
                        startActivity(Intent(this, StartServerActivity::class.java))
                    true
                }
                R.id.nav_media -> {
                    if (selectedItemId != R.id.nav_media)
                        startActivity(Intent(this, PicAudioActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}