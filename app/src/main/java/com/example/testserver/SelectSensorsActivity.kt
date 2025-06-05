package com.example.testserver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SelectSensorsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, DynamicSensorSettingsFragment())
            .commit()
    }
}