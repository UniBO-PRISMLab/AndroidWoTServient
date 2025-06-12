package com.example.testserver

import android.content.Context
import android.content.Intent
import android.util.Log

object SensorDataHolder {
    private val history = mutableMapOf<String, MutableList<Pair<Long, Float>>>()
    private var lastKnownProperties = emptySet<String>()

    fun addData(property: String, timestamp: Long, value: Float) {
        val list = history.getOrPut(property) { mutableListOf() }
        list.add(timestamp to value)
        if (list.size > 100) list.removeAt(0)
        checkForNewProperties()
    }

    fun getHistory(property: String): List<Pair<Long, Float>> {
        return history[property] ?: emptyList()
    }

    fun getAllProperties(): Set<String> {
        return history.keys
    }

    fun clearHistory() {
        history.clear()
        lastKnownProperties = emptySet()
    }

    fun removeProperty(property: String) {
        history.remove(property)
        checkForNewProperties()
    }

    private fun checkForNewProperties() {
        val currentProperties = history.keys
        if (currentProperties != lastKnownProperties) {
            lastKnownProperties = currentProperties.toSet()
            Log.d("SENSORDATAHOLDER", "Properties cambiate: $currentProperties")
        }
    }

    fun notifyPropertiesChanged(context: Context) {
        val intent = Intent("SENSOR_PROPERTIES_CHANGED")
        intent.putStringArrayListExtra("properties", ArrayList(getAllProperties()))
        context.sendBroadcast(intent)
        Log.d("SENSORDATAHOLDER", "Broadcast mandato per cambio properties")
    }

    fun getLatestValue(property: String): Float? {
        return history[property]?.lastOrNull()?.second
    }

    fun getDataCount(property: String): Int {
        return history[property]?.size ?: 0
    }
}