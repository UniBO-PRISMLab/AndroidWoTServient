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
    private lateinit var magnitudeStatusText: TextView

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val jsonNodeFactory = JsonNodeFactory.instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_sensor_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupInitialState()
        setupEventListeners()

        // Avvia connessione al dispositivo
        coroutineScope.launch {
            connectToThing()
        }
    }

    private fun initializeViews(view: View) {
        connectionStatus = view.findViewById(R.id.connectionStatus)
        progressBar = view.findViewById(R.id.progressBar)
        refreshButton = view.findViewById(R.id.refreshButton)
        actionsContainer = view.findViewById(R.id.actionsContainer)
        magnitudeButton = view.findViewById(R.id.magnitudeButton)

        wot = WoTClientHolder.wot!!

        // Crea il TextView per lo stato della magnitudine
        magnitudeStatusText = TextView(requireContext()).apply {
            text = "Magnitudine: Non calcolata"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(16, 16, 16, 16)
        }
    }

    private fun setupInitialState() {
        connectionStatus.text = "Connessione in corso..."
        progressBar.visibility = View.VISIBLE

        // Aggiungi il TextView della magnitudine al container
        actionsContainer.removeAllViews()
        actionsContainer.addView(magnitudeStatusText)

        // Inizialmente disabilita il pulsante finché non è connesso
        magnitudeButton.isEnabled = false
        magnitudeButton.alpha = 0.5f
    }

    private fun setupEventListeners() {
        refreshButton.setOnClickListener {
            refreshConnection()
        }

        magnitudeButton.setOnClickListener {
            getMagnitude()
        }
    }

    private fun refreshConnection() {
        setupInitialState()
        coroutineScope.launch {
            connectToThing()
        }
    }

    private suspend fun connectToThing() {
        try {
            withContext(Main) {
                connectionStatus.text = "Connessione al dispositivo..."
                progressBar.visibility = View.VISIBLE
            }

            // TODO: Controlla url corretto
            val url = "http://localhost:8080/smartphone"
            val td = wot.requestThingDescription(url)
            smartphoneThing = wot.consume(td)

            withContext(Main) {
                progressBar.visibility = View.GONE
                connectionStatus.text = "✓ Connesso al dispositivo"
                connectionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

                // Abilita il pulsante magnitudine
                magnitudeButton.isEnabled = true
                magnitudeButton.alpha = 1.0f
            }

            Log.d("SENSOR", "Connesso a Smartphone Thing")

        } catch (e: Exception) {
            withContext(Main) {
                progressBar.visibility = View.GONE
                connectionStatus.text = "✗ Errore di connessione"
                connectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))

                magnitudeButton.isEnabled = false
                magnitudeButton.alpha = 0.5f

                Toast.makeText(requireContext(), "Impossibile connettersi al dispositivo", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore connessione a Smartphone Thing: ", e)
        }
    }

    private fun getMagnitude() {
        // Disabilita temporaneamente il pulsante per evitare click multipli
        magnitudeButton.isEnabled = false
        magnitudeButton.alpha = 0.5f

        // Mostra stato di calcolo
        magnitudeStatusText.text = "Magnitudine: Calcolo in corso..."
        magnitudeStatusText.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))

        coroutineScope.launch {
            invokeGetMagnitude()
        }
    }

    private suspend fun invokeGetMagnitude() {
        try {
            val result = smartphoneThing?.invokeAction("getMagnitude")

            withContext(Main) {
                if (result != null) {
                    val magnitude = result.doubleValue() ?: -1.0

                    if (magnitude >= 0) {
                        magnitudeStatusText.text = "Magnitudine: ${String.format("%.2f", magnitude)}"
                        magnitudeStatusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                        Toast.makeText(requireContext(), "Magnitudine calcolata: ${String.format("%.2f", magnitude)}", Toast.LENGTH_SHORT).show()
                    } else {
                        magnitudeStatusText.text = "Magnitudine: Errore nel calcolo"
                        magnitudeStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                } else {
                    magnitudeStatusText.text = "Magnitudine: Risposta non valida"
                    magnitudeStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    Log.w("SENSOR", "Risultato nullo dall'azione getMagnitude")
                }

                // Riabilita il pulsante
                magnitudeButton.isEnabled = true
                magnitudeButton.alpha = 1.0f
            }

            Log.d("SENSOR", "getMagnitude invocata con successo!")

        } catch (e: Exception) {
            withContext(Main) {
                magnitudeStatusText.text = "Magnitudine: Errore di comunicazione"
                magnitudeStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))

                magnitudeButton.isEnabled = true
                magnitudeButton.alpha = 1.0f

                Toast.makeText(requireContext(), "Errore durante il calcolo della magnitudine", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore invocando getMagnitude: ", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancella eventuali coroutine in corso
        coroutineScope.coroutineContext[Job]?.cancel()
    }
}