package com.example.testserver

object ServientStats {
    var startTime = System.currentTimeMillis()
    var totalRequests = 0
    val requestCounts = mutableMapOf<String, Int>()
    val interactionTypes = mutableMapOf<String, Int>()
    val affordanceRequests = mutableMapOf<String, Int>()

    fun logRequest(thingId: String, interactionType: String, affordanceName: String? = null) {
        totalRequests++
        requestCounts[thingId] = requestCounts.getOrDefault(thingId, 0) +1
        interactionTypes[interactionType] = interactionTypes.getOrDefault(interactionType, 0) + 1
        if (affordanceName != null) {
            val affordanceKey = "$thingId.$affordanceName"
            affordanceRequests[affordanceKey] = affordanceRequests.getOrDefault(affordanceKey, 0) + 1
        }
    }

    fun uptimeSeconds(): Long = (System.currentTimeMillis() - startTime) / 1000

    fun getAffordanceStatistics(): Map<String, Int> {
        return affordanceRequests.toMap()
    }

    fun reset() {
        requestCounts.clear()
        interactionTypes.clear()
        affordanceRequests.clear()
        totalRequests = 0
    }
}