package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SensorDataFragment : Fragment() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sensorClient: MultiValueSensorClient? = null
    private val propertyViews = mutableMapOf<String, TextView>()

    private lateinit var connectionStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshButton: Button
    private lateinit var sensorDataContainer: LinearLayout

    private fun getFriendlyName(sensor: Sensor): String {
        return when (sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometro"
            Sensor.TYPE_LIGHT -> "Sensore di luminosità"
            Sensor.TYPE_GYROSCOPE -> "Giroscopio"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometro"
            Sensor.TYPE_PRESSURE -> "Barometro"
            Sensor.TYPE_PROXIMITY -> "Sensore di prossimità"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Sensore temperatura ambiente"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Sensore umidità"
            Sensor.TYPE_GRAVITY -> "Sensore gravità"
            else -> sensor.name
        }
    }

    private fun filterByNonWakeupSensors(sensors: List<Sensor>): List<Sensor> {
        val sensorsByType = sensors.groupBy { it.type }

        return sensorsByType.values.mapNotNull { sensorGroup ->
            when {
                sensorGroup.size == 1 -> sensorGroup.first() // Solo una versione disponibile
                sensorGroup.size > 1 -> {
                    // Cerca prima la versione non-wakeup
                    val nonWakeup = sensorGroup.find { !it.isWakeUpSensor }
                    nonWakeup ?: sensorGroup.first() // Se non trova non-wakeup, prende il primo
                }
                else -> null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_sensor_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectionStatus = view.findViewById(R.id.connectionStatus)
        progressBar = view.findViewById(R.id.progressBar)
        refreshButton = view.findViewById(R.id.refreshButton)
        sensorDataContainer = view.findViewById(R.id.sensorDataContainer)

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                connectionStatus.text = "In connessione.."
                progressBar.visibility = View.VISIBLE
            }

            val serverReady = waitForServerStart()
            if (!serverReady) {
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Server non disponibile dopo il timeout!"
                    progressBar.visibility = View.GONE
                }
                Log.e("DEBUG", "Server non disponibile dopo timeout")
                return@launch
            }

            try {
                val wot = WoTClientHolder.wot!!
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

                val allowedSensorTypes = listOf(
                    Sensor.TYPE_ACCELEROMETER,
                    Sensor.TYPE_LIGHT
                )

                val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
                    .filter { sensor -> sensor.type in allowedSensorTypes }
                val filteredSensors = filterByNonWakeupSensors(sensorList)

                val selectedProperties = mutableListOf<String>()
                val sensorDisplayNames = mutableMapOf<String, String>()

                for (sensor in filteredSensors) {
                    val key = "share_sensor_${sensor.name}"
                    if (!sharedPrefs.getBoolean(key, false)) continue

                    val sanitized = sensor.name.lowercase()
                        .replace("\\s+".toRegex(), "-")
                        .replace("[^a-z0-9\\-]".toRegex(), "") + "-${sensor.type}"

                    val friendlyName = getFriendlyName(sensor)
                    val sensorType = sensor.type
                    val numAxes = when (sensorType) {
                        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE,
                        Sensor.TYPE_GRAVITY, Sensor.TYPE_MAGNETIC_FIELD -> 3
                        else -> 1
                    }

                    if (numAxes == 1) {
                        selectedProperties.add(sanitized)
                        sensorDisplayNames[sanitized] = friendlyName
                    } else {
                        val axes = listOf("x", "y", "z")
                        for (axis in axes) {
                            val propName = "${sanitized}_$axis"
                            selectedProperties.add(propName)
                            sensorDisplayNames[propName] = "$friendlyName ($axis)"
                        }
                    }
                }

                val port = sharedPrefs.getString("server_port", "8080")?.toIntOrNull() ?: 8080
                val url = "http://localhost:$port/smartphone"
                val client = MultiValueSensorClient(wot, url)
                client.connect()
                sensorClient = client

                withContext(Dispatchers.Main) {
                    val titleView = TextView(requireContext()).apply {
                        textSize = 18f
                        text = "Smartphone: "
                        setPadding(8, 24, 8, 8)
                    }
                    sensorDataContainer.addView(titleView)

                    for (prop in selectedProperties) {
                        val displayName = sensorDisplayNames[prop] ?: prop
                        val valueView = TextView(requireContext()).apply {
                            textSize = 16f
                            text = "$displayName: ..."
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
            val timestamp = System.currentTimeMillis()
            withContext(Dispatchers.Main) {
                for ((prop, value) in values) {
                    val floatValue = (value as? Float) ?: -1f
                    val textView = propertyViews[prop]
                    if (textView != null) {
                        val currentText = textView.text.toString()
                        val displayName = currentText.substringBefore(":")
                        textView.text = if (floatValue == -1f)
                            "$displayName: Sensore non presente"
                        else
                            "$displayName: $value"
                    }
                    if (value != -1f) {
                        SensorDataHolder.addData(prop, timestamp, floatValue)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                propertyViews.values.forEach { textView ->
                    val currentText = textView.text.toString()
                    val displayName = currentText.substringBefore(":")
                    textView.text = "$displayName: errore"
                }
            }
        }
    }

    private suspend fun waitForServerStart(maxRetries: Int = 10, delayMillis: Long = 500): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        repeat(maxRetries) {
            if (prefs.getBoolean("server_started", false)) return true
            delay(delayMillis)
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}