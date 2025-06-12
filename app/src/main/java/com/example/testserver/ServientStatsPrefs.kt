package com.example.testserver

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject

object ServientStatsPrefs {
    private const val PREF_NAME = "servient_stats"

    fun save(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()

        // Questo potrei anche toglierlo
        editor.putLong("servient_start_time", ServientStats.startTime)
        editor.putInt("servient_total_requests", ServientStats.totalRequests)


        editor.putStringSet("request_counts_keys", ServientStats.requestCounts.keys)
        for ((key, value) in ServientStats.requestCounts) {
            editor.putInt("request_count_$key", value)
        }

        editor.putStringSet("interaction_types_keys", ServientStats.interactionTypes.keys)
        for ((key, value) in ServientStats.interactionTypes) {
            editor.putInt("interaction_type_$key", value)
        }

        editor.putStringSet("affordance_requests_keys", ServientStats.affordanceRequests.keys)
        for ((key, value) in ServientStats.affordanceRequests) {
            editor.putInt("affordance_request_$key", value)
        }

        editor.apply()
    }

    fun load(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        ServientStats.startTime = prefs.getLong("servient_start_time", System.currentTimeMillis())
        ServientStats.totalRequests = prefs.getInt("servient_total_requests", 0)

        ServientStats.requestCounts.clear()
        val requestCountsKeys = prefs.getStringSet("request_counts_keys", emptySet()) ?: emptySet()
        for (key in requestCountsKeys) {
            val value = prefs.getInt("request_count_$key", 0)
            ServientStats.requestCounts[key] = value
        }

        ServientStats.interactionTypes.clear()
        val interactionTypesKeys = prefs.getStringSet("interaction_types_keys", emptySet()) ?: emptySet()
        for (key in interactionTypesKeys) {
            val value = prefs.getInt("interaction_type_$key", 0)
            ServientStats.interactionTypes[key] = value
        }

        ServientStats.affordanceRequests.clear()
        val affordanceRequestsKeys = prefs.getStringSet("affordance_requests_keys", emptySet()) ?: emptySet()
        for (key in affordanceRequestsKeys) {
            val value = prefs.getInt("affordance_request_$key", 0)
            ServientStats.affordanceRequests[key] = value
        }
    }
}