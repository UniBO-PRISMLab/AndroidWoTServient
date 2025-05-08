package com.example.testserver

import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.thing.schema.WoTConsumedThing
import org.eclipse.thingweb.thing.schema.genericReadProperty
import java.net.URI

class CounterClient(
    private val wot: Wot,
    private val tdUrl: String
) {
    private lateinit var thing: WoTConsumedThing

    suspend fun connect() {
        val td = wot.requestThingDescription(URI(tdUrl))
        thing = wot.consume(td)
    }

    suspend fun getCounter(): Int = thing.genericReadProperty("counter")

    suspend fun increase() {
        thing.invokeAction("increase")
    }

    suspend fun decrease() {
        thing.invokeAction("decrease")
    }

    suspend fun reset() {
        thing.invokeAction("reset")
    }
}