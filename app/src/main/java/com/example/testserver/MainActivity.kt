package com.example.testserver

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.thing.schema.WoTConsumedThing
import org.eclipse.thingweb.thing.schema.genericReadProperty
import java.net.URI

class MainActivity : AppCompatActivity() {

    private lateinit var wot: Wot
    private lateinit var counterThing: WoTConsumedThing

    // IP casa
    // private val tdUrl = "http://192.168.1.4:8080/counter"

    // IP hotspot
    // private val tdUrl = "http://192.168.246.159:8080/counter"

    // NUOVO IP hotspot
    private val tdUrl = "http://192.168.227.159:8080/counter"

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val counterText = findViewById<TextView>(R.id.counterText)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val incrementButton = findViewById<Button>(R.id.incrementButton)
        val decreaseButton = findViewById<Button>(R.id.decreaseButton)
        val resetButton = findViewById<Button>(R.id.resetButton)

        coroutineScope.launch {
            val servient = Servient(
                clientFactories = listOf(HttpProtocolClientFactory())
            )
            wot = Wot.create(servient)

            // Scarica TD una volta sola
            val thingDescription = wot.requestThingDescription(URI(tdUrl))
            counterThing = wot.consume(thingDescription)

            withContext(Dispatchers.Main) {
                counterText.text = "WoT client inizializzato"
            }
        }


        refreshButton.setOnClickListener {
            coroutineScope.launch {
                val value = counterThing.genericReadProperty<Int>("counter")
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        incrementButton.setOnClickListener {
            coroutineScope.launch {
                counterThing.invokeAction("increase")
                val value = counterThing.genericReadProperty<Int>("counter")
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        decreaseButton.setOnClickListener {
            coroutineScope.launch {
                counterThing.invokeAction("decrease")
                val value = counterThing.genericReadProperty<Int>("counter")
                withContext(Dispatchers.Main) {
                    counterText.text = "Valore: $value"
                }
            }
        }

        resetButton.setOnClickListener {
            coroutineScope.launch {
                counterThing.invokeAction("reset")
                val value = counterThing.genericReadProperty<Int>("counter")
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
