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
    private val sensorClients = mutableMapOf<String, SingleValueSensorClient>()
    private val sensorViews = mutableMapOf<String, TextView>()

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
                    val url = "http://localhost:8080/$thingId"
                    val client = SingleValueSensorClient(wot, url)
                    try {
                        client.connect()
                        Log.d("DEBUG", "Trying to connect to: $url (sensor: ${sensor.name})")
                        sensorClients[thingId] = client

                        withContext(Dispatchers.Main) {
                            val textView = TextView(this@SensorDataActivity).apply {
                                textSize = 16f
                                text = "${sensor.name}: caricamento... "
                                setPadding(8, 16, 8, 16)
                            }
                            sensorViews[thingId] = textView
                            sensorDataContainer.addView(textView)
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
                val value = client.getSensorValue()
                withContext(Dispatchers.Main) {
                    sensorViews[thingId]?.text = "$thingId: $value"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sensorViews[thingId]?.text = "$thingId: errore"
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