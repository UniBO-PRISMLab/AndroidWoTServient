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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SensorDataActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sensorClient: MultiValueSensorClient? = null
    private val propertyViews = mutableMapOf<String, TextView>()

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
            val serverReady = waitForServerStart()
            if(!serverReady) {
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Server non disponibile dopo il timeout!"
                    progressBar.visibility = View.GONE
                }
                Log.e("DEBUG", "Server non disponibile dopo timeout")
                return@launch
            }

            try {
                val wot = WoTClientHolder.wot!!
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this@SensorDataActivity)
                val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)

                val selectedProperties = mutableListOf<String>()
                for (sensor in sensorList) {
                    val key = "share_sensor_${sensor.name}"
                    if (!sharedPrefs.getBoolean(key, false)) continue

                    val sanitized = sensor.name.lowercase()
                        .replace("\\s+".toRegex(), "-")
                        .replace("[^a-z0-9\\-]".toRegex(), "") + "-${sensor.type}"
                    val sensorType = sensor.type
                    val numAxes = when (sensorType) {
                        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GRAVITY, Sensor.TYPE_MAGNETIC_FIELD -> 3
                        else -> 1
                    }

                    if (numAxes == 1) {
                        selectedProperties.add(sanitized)
                    } else {
                        selectedProperties.add("${sanitized}_x")
                        selectedProperties.add("${sanitized}_y")
                        selectedProperties.add("${sanitized}_z")
                    }
                }

                val port = sharedPrefs.getString("server_port", "8080")?.toIntOrNull() ?: 8080
                val url = "http://localhost:$port/smartphone"
                val client = MultiValueSensorClient(wot, url)
                client.connect()
                sensorClient = client

                withContext(Dispatchers.Main) {
                    val titleView = TextView(this@SensorDataActivity).apply {
                        textSize = 18f
                        text = "Smartphone: "
                        setPadding(8, 24, 8, 8)
                    }
                    sensorDataContainer.addView(titleView)

                    for (prop in selectedProperties) {
                        val valueView = TextView(this@SensorDataActivity).apply {
                            textSize = 16f
                            text = "$prop: ..."
                            setPadding(16, 4, 8, 4)
                        }
                        sensorDataContainer.addView(valueView)
                        propertyViews[prop] = valueView
                    }

                    connectionStatus.text = "Connesso!"
                    progressBar.visibility = View.GONE
                }

                updateSensorValues()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Errore di connessione: ${e.message}"
                    progressBar.visibility = View.GONE
                }
                Log.e("DEBUG", "Errore durante la connessione: ", e)
            }
        }

        refreshButton.setOnClickListener {
            coroutineScope.launch {
                updateSensorValues()
            }
        }
    }

    private suspend fun updateSensorValues() {
        val client = sensorClient ?: return
        try {
            val values = client.getAllSensorValues()
            withContext(Dispatchers.Main) {
                for ((prop, value) in values) {
                    if (propertyViews.containsKey(prop)) {
                        if (value == -1f) {
                            propertyViews[prop]?.text = "$prop: Sensore non presente"
                        } else {
                            propertyViews[prop]?.text = "$prop: $value"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                propertyViews.values.forEach { it.text = "errore" }
            }
        }

    }

    private suspend fun waitForServerStart(maxRetries: Int = 10, delayMillis: Long = 500): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this@SensorDataActivity)
        repeat(maxRetries) {
            if (prefs.getBoolean("server_started", false)) return true
            delay(delayMillis)
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}