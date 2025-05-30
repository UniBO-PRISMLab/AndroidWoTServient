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

    /*
    // TODO Per funzionare singolo aggiungere property: String
    suspend fun getSensorValue(): Any? {
        return thing.genericReadProperty("value")
    }*/

    suspend fun getProperty(propertyName: String): Any? {
        return thing.genericReadProperty(propertyName)
    }

    suspend fun getAllProperties(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for ((name, _) in thing.properties) {
            result[name] = thing.genericReadProperty(name)
        }
        return result
    }
}
