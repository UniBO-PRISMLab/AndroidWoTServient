package com.example.testserver

import android.content.Intent
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
    private lateinit var counterClient: CounterClient
    private lateinit var sensorClient: SensorClient
    //TODO: usare IP giusto per parlare anche col PC
    private val counterTdUrl = "http://localhost:8080/counter"
    private val sensorTdUrl = "http://localhost:8080/sensor"

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

        val sensorText = findViewById<TextView>(R.id.sensorValueText2)
        // per ora l'ho tolto
        // val refreshSensorButton = findViewById<Button>(R.id.refreshSensorBtn2)

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                connectionStatus.text = "In connessione..."
                progressBar.visibility = View.VISIBLE
            }

            try {
                // Servient e Client nello stesso servient
                val servient = Servient(
                    servers = listOf(HttpProtocolServer()),
                    clientFactories = listOf(HttpProtocolClientFactory())
                )
                wot = Wot.create(servient)
                servient.start()

                // Avvia Server
                val server = Server(wot, servient, this@MainActivity)
                server.start()

                // Attendi che Thing sia esposto
                delay(500)

                // Avvia Client
                counterClient = CounterClient(wot, counterTdUrl)
                counterClient.connect()

                sensorClient = SensorClient(wot, sensorTdUrl)
                sensorClient.connect()

                // Leggi valori iniziali
                updateValues(counterText, sensorText)

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
                updateValues(counterText, sensorText)
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

    private suspend fun updateValues(counterText: TextView?, sensorText: TextView?) {
        val counter = counterClient.getCounter()
        val sensor = sensorClient.getSensorValue()

        withContext(Dispatchers.Main) {
            counterText?.text = "Counter: $counter"
            sensorText?.text = "Luminosità: $sensor lux"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // pulizia delle coroutine
    }
}
