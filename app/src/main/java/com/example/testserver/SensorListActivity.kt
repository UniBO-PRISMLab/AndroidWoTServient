package com.example.testserver

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.TextView

class SensorListActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        setContentView(textView)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        val sensorInfo = StringBuilder()
        sensorInfo.append("Sensori disponibili (${sensorList.size}): \n\n")

        for (sensor in sensorList) {
            sensorInfo.append("- ${sensor.name} (${sensor.type})\n")
        }

        textView.text = sensorInfo.toString()
    }
}