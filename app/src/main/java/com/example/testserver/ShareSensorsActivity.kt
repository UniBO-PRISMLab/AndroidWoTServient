package com.example.testserver

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShareSensorsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_sensors)

        val lightSensorCheckBox: CheckBox = findViewById(R.id.lightSensorCheckBox)
        val pressureSensorCheckBox: CheckBox = findViewById(R.id.pressureSensorCheckBox)
        val magnetometerCheckBox: CheckBox = findViewById(R.id.magnetometerCheckBox)
        val saveSettingsButton: Button = findViewById(R.id.saveSettingsButton)

        saveSettingsButton.setOnClickListener {
            val selected = listOfNotNull(
                if(lightSensorCheckBox.isChecked) "Luce" else null,
                if(pressureSensorCheckBox.isChecked) "Pressione" else null,
                if(magnetometerCheckBox.isChecked) "Magnetometro" else null
            )

            Toast.makeText(this, "Dati condivisi: ${selected.joinToString()}", Toast.LENGTH_SHORT).show()
            //TODO: salva le impostazioni da qualche parte (usare sharedPreferences)
        }
    }
}