package com.example.testserver

import android.content.Context
import org.json.JSONObject

object ServientStatsPrefs {
    private const val PREF_NAME = "servient_stats"

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Questo potrei anche toglierlo
        editor.putLong("startTime", ServientStats.startTime)

        editor.putInt("totalRequests", ServientStats.totalRequests)
        editor.putString("requestCounts", JSONObject(ServientStats.requestCounts as Map<*, *>).toString())
        editor.putString("interactionTypes", JSONObject(ServientStats.interactionTypes as Map<*, *>).toString())
        editor.apply()
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        ServientStats.startTime = prefs.getLong("startTime", System.currentTimeMillis())
        ServientStats.totalRequests = prefs.getInt("totalRequests", 0)
        ServientStats.requestCounts.clear()
        val reqJson = prefs.getString("requestCounts", "{}") ?: "{}"
        val reqObj = JSONObject(reqJson)
        reqObj.keys().forEach {
            ServientStats.requestCounts[it] = reqObj.getInt(it)
        }
        ServientStats.interactionTypes.clear()
        val intJson = prefs.getString("interactionTypes", "{}") ?: "{}"
        val intObj = JSONObject(intJson)
        intObj.keys().forEach {
            ServientStats.interactionTypes[it] = intObj.getInt(it)
        }
    }
}