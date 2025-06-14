package com.example.testserver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.thing.schema.WoTConsumedThing

class SensorActionsFragment : Fragment() {
    private lateinit var wot: Wot
    private var smartphoneThing: WoTConsumedThing? = null

    private lateinit var connectionStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshButton: Button
    private lateinit var actionsContainer: LinearLayout
    private lateinit var magnitudeButton: Button

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val jsonNodeFactory = JsonNodeFactory.instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Collegamento al layout XML
        return inflater.inflate(R.layout.activity_sensor_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectionStatus = view.findViewById(R.id.connectionStatus)
        progressBar = view.findViewById(R.id.progressBar)
        refreshButton = view.findViewById(R.id.refreshButton)
        actionsContainer = view.findViewById(R.id.actionsContainer)
        magnitudeButton = view.findViewById(R.id.magnitudeButton)

        wot = WoTClientHolder.wot!!
        coroutineScope.launch {
            connectToThing()
        }

        // Inizializzazione UI
        connectionStatus.text = "Connessione in corso..."
        progressBar.visibility = View.VISIBLE

        // Simulazione caricamento dati
        loadSensorData()

        // Gestione pulsante "Aggiorna Dati"
        refreshButton.setOnClickListener {
            loadSensorData()
        }

        magnitudeButton.setOnClickListener {
            getMagnitude()
        }
    }

    private fun loadSensorData() {
        // Simula un aggiornamento asincrono
        connectionStatus.text = "Caricamento dati..."
        progressBar.visibility = View.VISIBLE

        // Pulizia container
        actionsContainer.removeAllViews()

        // Simulazione ritardata (puoi usare Coroutine o Handler)
        actionsContainer.postDelayed({
            progressBar.visibility = View.GONE
            connectionStatus.text = "Dati aggiornati con successo"

            // Esempio: aggiunta dinamica di dati sensori
            val exampleSensorText = TextView(requireContext()).apply {
                text = "Luce: 220 lx\nPressione: 1013 hPa"
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.black, null))
            }

            actionsContainer.addView(exampleSensorText)
        }, 1500)
    }

    private suspend fun connectToThing() {
        try {
            // TODO Controlla url corretto
            val url = "http://localhost:8080/smartphone"
            val td = wot.requestThingDescription(url)
            smartphoneThing = wot.consume(td)
            Log.d("MEDIA", "Connesso a Smartphone Thing")
        } catch (e: Exception) {
            Log.e("MEDIA", "Errore connessione a Smartphone Thing: ", e)
        }
    }

    private fun getMagnitude() {
        coroutineScope.launch {
            invokeGetMagnitude()
        }
    }

    private suspend fun invokeGetMagnitude() {
        try {
            val result = smartphoneThing?.invokeAction("getMagnitude")
            if (result != null) {
                val magnitude = result.doubleValue() ?: -1.0
                withContext(Main) {
                    Toast.makeText(requireContext(), "$magnitude", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("ACTIONS", "Risultato nullo")
            }
            Log.d("ACTIONS", "getMagnitude invocata!")
        } catch (e: Exception) {
            Log.e("ACTIONS", "Errore invocando getMagnitude: ", e)
        }
    }
}