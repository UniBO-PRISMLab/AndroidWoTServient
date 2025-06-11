package com.example.testserver

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity() {

    private lateinit var mainContentText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_with_nav)
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
        val homeLayout = layoutInflater.inflate(R.layout.activity_main, contentFrame, false)
        contentFrame.addView(homeLayout)

        mainContentText = homeLayout.findViewById(R.id.main_content)
        // Mostra una breve introduzione nella home (main_content)
        mainContentText.text = "Benvenuto nella tua app WoT!\nUsa il menu in basso per navigare."

        setupBottomNavigation(R.id.nav_home)
    }
}
