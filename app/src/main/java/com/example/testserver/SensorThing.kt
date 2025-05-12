package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.eclipse.thingweb.reflection.annotations.Action
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing
import kotlin.random.Random

@Thing(
    id = "sensor",
    title = "Sensor Thing",
    description = "A Thing to expose a phone sensor"
)
class SensorThing(context: Context) : SensorEventListener {
    @Property(
        title = "Sensor Value",
        description = "Current sensor reading",
        readOnly = true
    )
    var sensorValue: Float = 0f

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    init {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_LIGHT) {
            sensorValue = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Action(
        title = "Refresh sensor",
        description = "Manually refresh sensor value"
    )
    fun refresh() {
        // Simulazione refresh, in pratica sarà settato da Android
        // sensorValue = Random.nextInt(0, 1001).toFloat()
    }

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}