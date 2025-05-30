package com.example.testserver

import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.thing.schema.WoTConsumedThing
import org.eclipse.thingweb.thing.schema.genericReadProperty
import java.net.URI

class GenericSensorClient(
    private val wot: Wot,
    private val url: String
) {
    private lateinit var thing: WoTConsumedThing

    suspend fun connect() {
        thing = wot.consume(wot.requestThingDescription(URI(url)))
    }

    suspend fun getSensorValue(property: String): Any? {
        return thing.genericReadProperty(property)
    }
}
