package com.example.testserver

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.binding.http.HttpProtocolServer

class MainActivity : AppCompatActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var wot: Wot

    // Client
    private lateinit var counterClient: CounterClient
    private lateinit var lightSensorClient: LightSensorClient
    private lateinit var pressureSensorClient: PressureSensorClient
    private lateinit var magnetometerClient: MagnetometerClient

    // Url
    //TODO: cambiare localhost per parlare anche col PC
    private val counterTdUrl = "http://localhost:8080/counter"
    private val lightSensorTdUrl = "http://localhost:8080/light-sensor"
    private val pressureSensorTdUrl = "http://localhost:8080/pressure-sensor"
    private val magnetometerTdUrl = "http://localhost:8080/magnetometer-sensor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val counterText = findViewById<TextView>(R.id.counterText)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val incrementButton = findViewById<Button>(R.id.incrementButton)
        val decreaseButton = findViewById<Button>(R.id.decreaseButton)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val connectionStatus = findViewById<TextView>(R.id.connectionStatus)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val lightSensorText = findViewById<TextView>(R.id.sensorValueText)
        val pressureText = findViewById<TextView>(R.id.pressureValueText)
        val magnetometerText = findViewById<TextView>(R.id.sensorMagnetoText)

        // Lista Sensori
        val sensorListButton: Button = findViewById(R.id.sensorListButton)

        // TODO: NEW
        // Prima di avviare foreground service devo chiedere permesso per notifica
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        // Avvia foreground service
        val serviceIntent = Intent(this, WoTService::class.java)
        startForegroundService(serviceIntent)

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                connectionStatus.text = "In connessione..."
                progressBar.visibility = View.VISIBLE
            }

            try {
                // Attendi che Server sia attivo
                delay(1000)

                // Avvia i client
                val wot = WoTClientHolder.wot!!
                counterClient = CounterClient(wot, counterTdUrl)
                counterClient.connect()
                lightSensorClient = LightSensorClient(wot, lightSensorTdUrl)
                lightSensorClient.connect()
                pressureSensorClient = PressureSensorClient(wot, pressureSensorTdUrl)
                pressureSensorClient.connect()
                magnetometerClient = MagnetometerClient(wot, magnetometerTdUrl)
                magnetometerClient.connect()

                // Leggi valori iniziali
                updateValues(counterText, lightSensorText, pressureText, magnetometerText)

                // Aggiorna TextView con connessione riuscita
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "Connesso!"
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "❌ Errore: ${e.message}"
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

        sensorListButton.setOnClickListener {
            val intent = Intent(this, SensorListActivity::class.java)
            startActivity(intent)
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
        coroutineScope.cancel() // Pulizia delle Coroutine
    }
}