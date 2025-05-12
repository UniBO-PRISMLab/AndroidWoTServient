package com.example.testserver

import org.eclipse.thingweb.reflection.annotations.Action
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing
import kotlin.random.Random

@Thing(
    id = "sensor",
    title = "Sensor Thing",
    description = "A Thing to expose a phone sensor"
)
class SensorThing {
    @Property(
        title = "Sensor Value",
        description = "Current sensor reading",
        readOnly = true
    )
    var sensorValue: Float = 0f

    @Action(
        title = "Refresh sensor",
        description = "Manually refresh sensor value"
    )
    fun refresh() {
        // Simulazione refresh, in pratica sarà settato da Android
        sensorValue = Random.nextInt(0, 1001).toFloat()
    }
}