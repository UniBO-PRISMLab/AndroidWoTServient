package com.example.testserver

object SensorDataHolder {
    private val history = mutableMapOf<String, MutableList<Pair<Long, Float>>>()

    fun addData(property: String, timestamp: Long, value: Float) {
        val list = history.getOrPut(property) { mutableListOf() }
        list.add(timestamp to value)
        if (list.size > 100) list.removeAt(0)
    }

    fun getHistory(property: String): List<Pair<Long, Float>> {
        return history[property] ?: emptyList()
    }

    fun getAllProperties(): Set<String> {
        return history.keys
    }
}