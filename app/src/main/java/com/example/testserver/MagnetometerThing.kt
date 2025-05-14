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
    id = "magnetometer-sensor",
    title = "Magnetometer Sensor Thing",
    description = "A Thing to expose the phone's magnetic field sensor"
)
class MagnetometerThing(context: Context) : SensorEventListener {
    @Property(
        title = "Magnetic field X",
        description = "Magnetic field strength on X axis in microT",
        readOnly = true
    )
    var magneticFieldX: Float = 0f

    @Property(
        title = "Magnetic field Y",
        description = "Magnetic field strength on Y axis in microT",
        readOnly = true
    )
    var magneticFieldY: Float = 0f

    @Property(
        title = "Magnetic field Z",
        description = "Magnetic field strength on Z axis in microT",
        readOnly = true
    )
    var magneticFieldZ: Float = 0f

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    init {
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldX = event.values[0]
            magneticFieldY = event.values[1]
            magneticFieldZ = event.values[2]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Action(
        title = "Refresh magnetic field",
        description = "Manually refresh magnetic field values"
    )
    fun refresh() {
        // I valori si aggiornano da soli..
    }

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}