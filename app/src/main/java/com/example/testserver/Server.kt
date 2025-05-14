package com.example.testserver

import android.content.Context
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.reflection.ExposedThingBuilder
import org.eclipse.thingweb.thing.schema.WoTExposedThing

class Server(
    private val wot: Wot,
    private val servient: Servient,
    private val context: Context
) {
    suspend fun start(): List<WoTExposedThing> {
        val exposedThings = mutableListOf<WoTExposedThing>()

        // Counter
        val counterThing = CounterThing()
        val exposedCounter = ExposedThingBuilder.createExposedThing(wot, counterThing, CounterThing::class)
        if(exposedCounter != null) {
            servient.addThing(exposedCounter)
            servient.expose(exposedCounter.getThingDescription().id)
            exposedThings.add(exposedCounter)
        }

        // Light Sensor
        val lightSensorThing = LightSensorThing(context)
        val exposedLightSensor = ExposedThingBuilder.createExposedThing(wot, lightSensorThing, LightSensorThing::class)
        if(exposedLightSensor != null) {
            servient.addThing(exposedLightSensor)
            servient.expose(exposedLightSensor.getThingDescription().id)
            exposedThings.add(exposedLightSensor)
        }

        // Pressure Sensor -- NON ESISTE SUL DISPOSITIVO -- RIMUOVERE
        val pressureSensorThing = PressureSensorThing(context)
        val exposedPressureSensor = ExposedThingBuilder.createExposedThing(wot, pressureSensorThing, PressureSensorThing::class)
        if(exposedPressureSensor != null) {
            servient.addThing(exposedPressureSensor)
            servient.expose(exposedPressureSensor.getThingDescription().id)
            exposedThings.add(exposedPressureSensor)
        }

        // Magnetometer
        val magnetometerThing = MagnetometerThing(context)
        val exposedMagnetometer = ExposedThingBuilder.createExposedThing(wot, magnetometerThing, MagnetometerThing::class)
        if(exposedMagnetometer != null) {
            servient.addThing(exposedMagnetometer)
            servient.expose(exposedMagnetometer.getThingDescription().id)
            exposedThings.add(exposedMagnetometer)
        }

        return exposedThings
    }
}