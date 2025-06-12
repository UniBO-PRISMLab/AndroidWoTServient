package com.example.testserver

import android.content.Context

object ServientStats {
    var startTime = System.currentTimeMillis()
    var totalRequests = 0
    val requestCounts = mutableMapOf<String, Int>()
    val interactionTypes = mutableMapOf<String, Int>()
    val affordanceRequests = mutableMapOf<String, Int>()

    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context
        loadFromPrefs()
    }

    fun logRequest(thingId: String, interactionType: String, affordanceName: String? = null) {
        totalRequests++
        requestCounts[thingId] = requestCounts.getOrDefault(thingId, 0) + 1
        interactionTypes[interactionType] = interactionTypes.getOrDefault(interactionType, 0) + 1
        if (affordanceName != null) {
            val affordanceKey = "$thingId.$affordanceName"
            affordanceRequests[affordanceKey] = affordanceRequests.getOrDefault(affordanceKey, 0) + 1
        }

        // Salva automaticamente dopo ogni richiesta
        context?.let { ServientStatsPrefs.save(it) }
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
        startTime = System.currentTimeMillis()

        // Salva il reset
        context?.let { ServientStatsPrefs.save(it) }
    }

    private fun loadFromPrefs() {
        context?.let { ServientStatsPrefs.load(it) }
    }

    fun saveToPrefs() {
        context?.let { ServientStatsPrefs.save(it) }
    }
}