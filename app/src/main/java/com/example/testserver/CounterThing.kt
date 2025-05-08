package com.example.testserver

import org.eclipse.thingweb.reflection.annotations.Action
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing

@Thing(
    id = "counter",
    title = "Counter Thing",
    description = "A simple Counter"
)
class CounterThing {
    @Property(
        title = "counter",
        readOnly = true
    )
    var counter: Int = 0

    @Action(
        title = "Increase Counter",
        description = "Increase Counter by 1"
    )
    fun increase() {
        counter++
    }

    @Action(
        title = "Decrease Counter",
        description = "Decrease Counter by 1"
    )
    fun decrease() {
        counter--
    }

    @Action(
        title = "Reset Counter",
        description = "Reset Counter to 0"
    )
    fun reset() {
        counter = 0
    }
}