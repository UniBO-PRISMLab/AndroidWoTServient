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

    // TODO Per funzionare singolo aggiungere property: String
    suspend fun getSensorValue(): Any? {
        return thing.genericReadProperty("value")
    }
}
