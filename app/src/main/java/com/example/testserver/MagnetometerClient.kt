package com.example.testserver

import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.thing.schema.WoTConsumedThing
import org.eclipse.thingweb.thing.schema.genericReadProperty
import java.net.URI

class MagnetometerClient(
    private val wot: Wot,
    private val tdUrl: String
) {
    private lateinit var thing: WoTConsumedThing

    suspend fun connect() {
        val td = wot.requestThingDescription(URI(tdUrl))
        thing = wot.consume(td)
    }

    suspend fun getMagneticField(): Triple<Float, Float, Float> {
        val x = thing.genericReadProperty<Float>("magneticFieldX")
        val y = thing.genericReadProperty<Float>("magneticFieldY")
        val z = thing.genericReadProperty<Float>("magneticFieldZ")
        return Triple(x, y, z)
    }
}