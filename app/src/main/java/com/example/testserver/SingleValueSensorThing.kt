package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing

@Thing(
    id = "single-value-sensor",
    title = "Single Value Sensor Thing",
    description = "A Sensor that returns a single float value"
)
class SingleValueSensorThing(
    context: Context,
    private val sensorType: Int,
    private val sensorName: String
) : SensorEventListener {

    @Property(
        title = "value",
        description = "Sensor value",
        readOnly = true
    )
    var value: Float = 0f

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)

    init {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == sensorType) {
            value = event.values.getOrNull(0) ?: 0f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}