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
    private var title: String = ""

    suspend fun connect() {
        val td = wot.requestThingDescription(URI(url))
        title = td.title ?: "Unknown Thing"
        thing = wot.consume(td)
    }

    suspend fun getSensorValue(): List<Float>? {
        return thing.genericReadProperty("value") as? List<Float>
    }
}
