package com.example.testserver

import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.reflection.ExposedThingBuilder
import org.eclipse.thingweb.thing.schema.WoTExposedThing

class CounterServer(
    private val wot: Wot,
    private val servient: Servient
) {
    suspend fun start(): WoTExposedThing? {
        val counterThing = CounterThing()
        val exposedThing = ExposedThingBuilder.createExposedThing(wot, counterThing, CounterThing::class)
        if(exposedThing != null) {
            servient.addThing(exposedThing)
            servient.expose(exposedThing.getThingDescription().id)
        }
        return exposedThing
    }
}