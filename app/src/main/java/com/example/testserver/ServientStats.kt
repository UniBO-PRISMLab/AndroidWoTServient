package com.example.testserver

object ServientStats {
    var startTime = System.currentTimeMillis()
    var totalRequests = 0
    val requestCounts = mutableMapOf<String, Int>()
    val interactionTypes = mutableMapOf<String, Int>()

    fun logRequest(thingName: String, interactionType: String) {
        totalRequests++
        requestCounts[thingName] = (requestCounts[thingName] ?: 0) + 1
        interactionTypes[interactionType] = (interactionTypes[interactionType] ?: 0) + 1
    }

    fun uptimeSeconds(): Long = (System.currentTimeMillis() - startTime) / 1000
}