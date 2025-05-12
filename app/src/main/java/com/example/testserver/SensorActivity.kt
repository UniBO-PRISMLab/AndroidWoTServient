package com.example.testserver

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.binding.http.HttpProtocolServer

class SensorActivity: Activity(), SensorEventListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var wot: Wot
    private lateinit var client: SensorClient
    private val tdUrl = "http://localhost:8080/sensor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        val valueText = findViewById<TextView>(R.id.sensorValueText)
        val refreshButton = findViewById<Button>(R.id.refreshSensorBtn)

        coroutineScope.launch {
            client = SensorClient(wot, tdUrl)
            client.connect()

            val value = client.getSensorValue()
            withContext(Dispatchers.Main) {
                valueText.text = "Luminosità: $value lux"
            }
        }

        refreshButton.setOnClickListener {
            coroutineScope.launch {
                val value = client.getSensorValue()
                withContext(Dispatchers.Main) {
                    valueText.text = "Luminosità: $value lux"
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        TODO("Not yet implemented")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}