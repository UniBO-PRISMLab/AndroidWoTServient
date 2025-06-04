package com.example.testserver

import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.thing.schema.WoTConsumedThing
import org.eclipse.thingweb.thing.schema.genericReadProperty
import java.net.URI

class SingleValueSensorClient(
    private val wot: Wot,
    private val url: String
) {
    private lateinit var thing: WoTConsumedThing

    suspend fun connect() {
        val td = wot.requestThingDescription(URI(url))
        thing = wot.consume(td)
    }

    suspend fun getSensorValue(): Float? {
        return thing.genericReadProperty("value") as? Float
    }
}