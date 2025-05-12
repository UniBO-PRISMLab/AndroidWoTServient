package com.example.testserver

import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.reflection.ExposedThingBuilder
import org.eclipse.thingweb.thing.schema.WoTExposedThing

class Server(
    private val wot: Wot,
    private val servient: Servient
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

        // Sensor
        val sensorThing = SensorThing()
        val exposedSensor = ExposedThingBuilder.createExposedThing(wot, sensorThing, SensorThing::class)
        if(exposedSensor != null) {
            servient.addThing(exposedSensor)
            servient.expose(exposedSensor.getThingDescription().id)
            exposedThings.add(exposedSensor)
        }

        return exposedThings
    }
}