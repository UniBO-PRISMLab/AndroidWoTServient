package com.example.testserver

import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.reflection.ExposedThingBuilder
import org.eclipse.thingweb.thing.schema.WoTExposedThing

class SensorServer(
    private val wot: Wot,
    private val servient: Servient,
    private val sensorThing: SensorThing
) {
    suspend fun start(): WoTExposedThing? {
        val exposedThing = ExposedThingBuilder.createExposedThing(wot, sensorThing, SensorThing::class)
        if(exposedThing != null) {
            servient.addThing(exposedThing)
            servient.expose(exposedThing.getThingDescription().id)
        }
        return exposedThing
    }
}