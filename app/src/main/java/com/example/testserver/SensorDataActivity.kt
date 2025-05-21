package com.example.testserver

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SensorDataActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var counterClient: CounterClient
    private lateinit var lightSensorClient: LightSensorClient
    private lateinit var pressureSensorClient: PressureSensorClient
    private lateinit var magnetometerClient: MagnetometerClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_data)

        // Stato connessione
        val connectionStatus = findViewById<TextView>(R.id.connectionStatus)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val counterText = findViewById<TextView>(R.id.counterText)
        val lightSensorText = findViewById<TextView>(R.id.sensorValueText)
        val pressureText = findViewById<TextView>(R.id.pressureValueText)
        val magnetometerText = findViewById<TextView>(R.id.sensorMagnetoText)

        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val incrementButton = findViewById<Button>(R.id.incrementButton)
        val decreaseButton = findViewById<Button>(R.id.decreaseButton)
        val resetButton = findViewById<Button>(R.id.resetButton)

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                connectionStatus.text = "In connessione.."
                progressBar.visibility = View.VISIBLE
            }
            try {
                val wot = WoTClientHolder.wot!!
                counterClient = CounterClient(wot, "http://localhost:8080/counter")
                lightSensorClient = LightSensorClient(wot, "http://localhost:8080/light-sensor")
                pressureSensorClient =
                    PressureSensorClient(wot, "http://localhost:8080/pressure-sensor")
                magnetometerClient =
                    MagnetometerClient(wot, "http://localhost:8080/magnetometer-sensor")

                counterClient.connect()
                lightSensorClient.connect()
                pressureSensorClient.connect()
                magnetometerClient.connect()

                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Connesso!"
                    progressBar.visibility = View.GONE
                }

                updateValues(counterText, lightSensorText, pressureText, magnetometerText)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Errore di connessione!"
                    progressBar.visibility = View.GONE
                }
            }
        }

        refreshButton.setOnClickListener {
            coroutineScope.launch {
                updateValues(counterText, lightSensorText, pressureText, magnetometerText)
            }
        }

        incrementButton.setOnClickListener {
            coroutineScope.launch {
                counterClient.increase()
                val value = counterClient.getCounter()
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        decreaseButton.setOnClickListener {
            coroutineScope.launch {
                counterClient.decrease()
                val value = counterClient.getCounter()
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        resetButton.setOnClickListener {
            coroutineScope.launch {
                counterClient.reset()
                val value = counterClient.getCounter()
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }
    }

    private suspend fun updateValues(
        counterText: TextView?,
        lightSensorText: TextView?,
        pressureText: TextView?,
        magnetometerText: TextView?) {
        val counter = counterClient.getCounter()
        val lightSensor = lightSensorClient.getLightLevel()
        val pressure = pressureSensorClient.getPressure()
        val magneticField = magnetometerClient.getMagneticField()

        withContext(Dispatchers.Main) {
            counterText?.text = "Counter: $counter"
            lightSensorText?.text = "Luminosità: $lightSensor lux"
            pressureText?.text = "Pressione: $pressure hPa"
            magnetometerText?.text = "Campo magnetico: \nX: ${magneticField.first} µT\nY: ${magneticField.second} µT\nZ: ${magneticField.third} µT\n"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}