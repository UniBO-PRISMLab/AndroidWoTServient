package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SensorDataActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sensorClients = mutableMapOf<String, MultiValueSensorClient>()
    private val sensorViews = mutableMapOf<String, MutableMap<String, TextView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_data)

        val connectionStatus = findViewById<TextView>(R.id.connectionStatus)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val sensorDataContainer = findViewById<LinearLayout>(R.id.sensorDataContainer)

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                connectionStatus.text = "In connessione.."
                progressBar.visibility = View.VISIBLE
            }
            try {
                val wot = WoTClientHolder.wot!!
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this@SensorDataActivity)
                val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)

                for (sensor in sensorList) {
                    val key = "share_sensor_${sensor.name}"
                    if(!sharedPrefs.getBoolean(key, false)) continue

                    val thingId = sanitizeSensorName(sensor.name, sensor.type)
                    val port = sharedPrefs.getString("server_port", "8080")?.toIntOrNull() ?: 8080
                    val url = "http://localhost:$port/$thingId"
                    val client = MultiValueSensorClient(wot, url)
                    try {
                        client.connect()
                        Log.d("DEBUG", "Trying to connect to: $url (sensor: ${sensor.name})")
                        sensorClients[thingId] = client

                        withContext(Dispatchers.Main) {
                            val propertyViews = mutableMapOf<String, TextView>()
                            val titleView = TextView(this@SensorDataActivity).apply {
                                textSize = 16f
                                text = "${sensor.name}:"
                                setPadding(8, 24, 8, 8)
                            }
                            sensorDataContainer.addView(titleView)

                            val values = client.getAllSensorValues()
                            for((prop, _) in values) {
                                val valueView = TextView(this@SensorDataActivity).apply {
                                    textSize = 16f
                                    text = "$prop: ..."
                                    setPadding(16, 4, 8, 4)
                                }
                                sensorDataContainer.addView(valueView)
                                propertyViews[prop] = valueView
                            }
                            sensorViews[thingId] = propertyViews
                        }
                    } catch (e: Exception) {
                        Log.e("DEBUG", "Errore connessione a &url", e)
                        withContext(Dispatchers.Main) {
                            val textView = TextView(this@SensorDataActivity).apply {
                                textSize = 16f
                                text = "${sensor.name}: errore di connessione - ${e.message ?: "Errore sconosciuto"}"
                                setPadding(8, 16, 8, 16)
                            }
                            sensorDataContainer.addView(textView)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Connesso!"
                    progressBar.visibility = View.GONE

                }

                updateAllSensorValues()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Errore di connessione: ${e.message}"
                    progressBar.visibility = View.GONE
                }
            }
        }

        refreshButton.setOnClickListener {
            coroutineScope.launch {
                updateAllSensorValues()
            }
        }
    }

    private suspend fun updateAllSensorValues() {
        for ((thingId, client) in sensorClients) {
            try {
                val values = client.getAllSensorValues()
                withContext(Dispatchers.Main) {
                    val views = sensorViews[thingId]
                    values.forEach { (prop, value) ->
                        if(value == -1f) {
                            views?.get(prop)?.text = "$prop: Sensore non presente/non funzionante"
                        } else {
                            views?.get(prop)?.text = "$prop: $value"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sensorViews[thingId]?.values?.forEach { it.text = "errore" }
                }
            }
        }
    }

    private fun sanitizeSensorName(name: String, type: Int): String {
        val sanitizedName = name.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
        return "$sanitizedName-$type"
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}