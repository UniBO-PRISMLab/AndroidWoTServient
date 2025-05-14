package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.eclipse.thingweb.reflection.annotations.Action
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing

@Thing(
    id = "light-sensor",
    title = "Light Sensor Thing",
    description = "A Thing to expose the phone's ambient light sensor"
)
class LightSensorThing(context: Context) : SensorEventListener {
    @Property(
        title = "Light level",
        description = "Current ambient light level in lux",
        readOnly = true
    )
    var lightValue: Float = 0f

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    init {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_LIGHT) {
            lightValue = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Action(
        title = "Refresh light level",
        description = "Manually refresh light sensor value"
    )
    fun refresh() {
        // niente, si aggiorna da solo?
    }

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}