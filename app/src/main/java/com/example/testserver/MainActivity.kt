package com.example.testserver

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
    private lateinit var client: CounterClient
    //TODO: usare IP giusto per parlare anche col PC
    private val tdUrl = "http://localhost:8080/counter"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val counterText = findViewById<TextView>(R.id.counterText)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val incrementButton = findViewById<Button>(R.id.incrementButton)
        val decreaseButton = findViewById<Button>(R.id.decreaseButton)
        val resetButton = findViewById<Button>(R.id.resetButton)

        // NEW
        val connectionStatus = findViewById<TextView>(R.id.connectionStatus)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

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
                val server = CounterServer(wot, servient)
                server.start()

                // Attendi che Thing sia esposto
                delay(500)

                // Avvia Client
                client = CounterClient(wot, tdUrl)
                client.connect()

                val initialValue = client.getCounter()
                withContext(Dispatchers.Main) {
                    connectionStatus.text = "✅ Connesso"
                    progressBar.visibility = View.GONE
                    counterText.text = "Valore: $initialValue"
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
                val value = client.getCounter()
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        incrementButton.setOnClickListener {
            coroutineScope.launch {
                client.increase()
                val value = client.getCounter()
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        decreaseButton.setOnClickListener {
            coroutineScope.launch {
                client.decrease()
                val value = client.getCounter()
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        resetButton.setOnClickListener {
            coroutineScope.launch {
                client.reset()
                val value = client.getCounter()
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // pulizia delle coroutine
    }

}
