package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing

@Thing(
    id = "generic-sensor",
    title = "Generic Sensor Thing",
    description = "Generic sensor Thing"
)
class GenericSensorThing(
    context: Context,
    private val sensorType: Int,
    private val sensorName: String
) : SensorEventListener {

    @Property(
        title = "x",
        description = "X-axis value (or main value for single-axis sensors)",
        readOnly = true
    )
    var x: Float = 0f

    @Property(
        title = "y",
        description = "Y-axis value (0 if not applicable)",
        readOnly = true
    )
    var y: Float = 0f

    @Property(
        title = "z",
        description = "Z-axis value (0 if not applicable)",
        readOnly = true
    )
    var z: Float = 0f

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)

    init {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == sensorType) {
            x = event.values.getOrNull(0) ?: 0f
            y = event.values.getOrNull(1) ?: 0f
            z = event.values.getOrNull(2) ?: 0f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}