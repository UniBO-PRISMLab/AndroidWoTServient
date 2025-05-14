package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import org.eclipse.thingweb.reflection.annotations.Action
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing

@Thing(
    id = "pressure-sensor",
    title = "Pressure Sensor Thing",
    description = "A Thing to expose the phone's barometric pressure sensor"
)
class PressureSensorThing(context: Context) : SensorEventListener {
    @Property(
        title = "Pressure",
        description = "Current atmospheric pressure in hPa",
        readOnly = true
    )
    var pressureValue: Float = 0f

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    init {
        val hasPressure = pressureSensor != null
        Log.d("SENSOR", "Pressure sensor available: $hasPressure")
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            pressureValue = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Action(
        title = "Refresh pressure",
        description = "Manually refresh pressure value"
    )
    fun refresh() {
        // Si aggiorna da solo, non serve implementare la funzione
    }

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}