package com.example.testserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

    private lateinit var northButton: Button
    private lateinit var northStatusText: TextView

    private lateinit var orientationButton: Button
    private lateinit var orientationStatusText: TextView

    private lateinit var inclinationButton: Button
    private lateinit var inclinationStatusText: TextView

    private lateinit var inPocketButton: Button
    private lateinit var inPocketStatusText: TextView

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val jsonNodeFactory = JsonNodeFactory.instance

    private val originalButtonTexts = mutableMapOf<Button, String>()

    // BroadcastReceiver per ascoltare i cambi di preferenze
    private val preferencesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "PREFERENCES_UPDATED") {
                val updateType = intent.getStringExtra("update_type")
                if (updateType == "sensors") {
                    Log.d("SENSOR_ACTIONS", "Aggiornamento preferenze sensori ricevuto")
                    updateButtonStates()
                }
            }
        }
    }

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

        // Registra il BroadcastReceiver
        val filter = IntentFilter("PREFERENCES_UPDATED")
        requireActivity().registerReceiver(preferencesReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

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
        northButton = view.findViewById(R.id.northButton)
        orientationButton = view.findViewById(R.id.orientationButton)
        inclinationButton = view.findViewById(R.id.inclinationButton)
        inPocketButton = view.findViewById(R.id.inPocketButton)

        originalButtonTexts[magnitudeButton] = magnitudeButton.text.toString()
        originalButtonTexts[northButton] = northButton.text.toString()
        originalButtonTexts[orientationButton] = orientationButton.text.toString()
        originalButtonTexts[inclinationButton] = inclinationButton.text.toString()
        originalButtonTexts[inPocketButton] = inPocketButton.text.toString()

        wot = WoTClientHolder.wot!!

        magnitudeStatusText = TextView(requireContext()).apply {
            text = "Magnitudine: Non calcolata"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(16, 16, 16, 16)
        }
        northStatusText = TextView(requireContext()).apply {
            text = "Direzione Nord: Non calcolata"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(16, 16, 16, 16)
        }
        orientationStatusText = TextView(requireContext()).apply {
            text = "Orientamento: Non calcolato"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(16, 16, 16, 16)
        }
        inclinationStatusText = TextView(requireContext()).apply {
            text = "Inclinazione: Non calcolata"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(16, 16, 16, 16)
        }
        inPocketStatusText = TextView(requireContext()).apply {
            text = "In tasca: Non verificato"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(16, 16, 16, 16)
        }
    }

    private fun setupInitialState() {
        connectionStatus.text = "Connessione in corso..."
        progressBar.visibility = View.VISIBLE

        // Aggiungi i TextView al container
        actionsContainer.removeAllViews()
        actionsContainer.addView(magnitudeStatusText)
        actionsContainer.addView(northStatusText)
        actionsContainer.addView(orientationStatusText)
        actionsContainer.addView(inclinationStatusText)
        actionsContainer.addView(inPocketStatusText)

        // Aggiorna gli stati dei bottoni in base alle preferenze
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Mappa i bottoni ai tipi di sensori richiesti e ai messaggi per bottoni disabilitati
        val buttonConfig = mapOf(
            magnitudeButton to ButtonConfig(
                requiredSensorTypes = listOf(Sensor.TYPE_ACCELEROMETER),
                disabledMessage = "Calcola magnitudine (Attiva sensore)"
            ),
            northButton to ButtonConfig(
                requiredSensorTypes = listOf(Sensor.TYPE_MAGNETIC_FIELD),
                disabledMessage = "Calcola direzione nord (Attiva sensore)"
            ),
            orientationButton to ButtonConfig(
                requiredSensorTypes = listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD),
                disabledMessage = "Calcola orientamento (Attiva sensori)"
            ),
            inclinationButton to ButtonConfig(
                requiredSensorTypes = listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD),
                disabledMessage = "Calcola inclinazione (Attiva sensori)"
            ),
            inPocketButton to ButtonConfig(
                requiredSensorTypes = listOf(Sensor.TYPE_PROXIMITY, Sensor.TYPE_LIGHT),
                disabledMessage = "Verifica in tasca (Attiva sensori)"
            )
        )

        buttonConfig.forEach { (button, config) ->
            val isEnabled = when (config.requiredSensorTypes.size) {
                1 -> {
                    isSensorEnabled(sensorManager, config.requiredSensorTypes.first(), sharedPrefs)
                }
                else -> {
                    config.requiredSensorTypes.all { sensorType ->
                        isSensorEnabled(sensorManager, sensorType, sharedPrefs)
                    }
                }
            }

            button.isEnabled = isEnabled
            button.alpha = if (isEnabled) 1.0f else 0.5f

            // Aggiorna il testo del bottone
            button.text = if (isEnabled) {
                originalButtonTexts[button] ?: button.text.toString()
            } else {
                config.disabledMessage
            }
        }
    }

    private data class ButtonConfig(
        val requiredSensorTypes: List<Int>,
        val disabledMessage: String
    )

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

    private fun isSensorEnabled(sensorManager: SensorManager, sensorType: Int, sharedPrefs: android.content.SharedPreferences): Boolean {
        val allSensors = sensorManager.getSensorList(sensorType)
        val filteredSensors = filterByNonWakeupSensors(allSensors)

        Log.d("SENSOR_CHECK", "Checking sensor type $sensorType, found ${allSensors.size} total sensors, ${filteredSensors.size} after filtering")

        val result = filteredSensors.any { sensor ->
            val key = "share_sensor_${sensor.name}"
            val isEnabled = sharedPrefs.getBoolean(key, false)
            Log.d("SENSOR_CHECK", "Sensore filtrato ${sensor.name} (tipo $sensorType, key=$key): $isEnabled")
            isEnabled
        }

        Log.d("SENSOR_CHECK", "Final result for sensor type $sensorType: $result")
        return result
    }

    private fun showSensorNotEnabledToast(sensorNames: List<String>) {
        val message = if (sensorNames.size == 1) {
            "Attiva il sensore ${sensorNames.first()} per utilizzare questa funzione"
        } else {
            "Attiva almeno uno dei sensori: ${sensorNames.joinToString(", ")} per utilizzare questa funzione"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun setupEventListeners() {
        refreshButton.setOnClickListener {
            refreshConnection()
        }

        magnitudeButton.setOnClickListener {
            if (magnitudeButton.isEnabled) {
                getMagnitude()
            } else {
                showSensorNotEnabledToast(listOf("Accelerometro"))
            }
        }

        northButton.setOnClickListener {
            if (northButton.isEnabled) {
                getNorthDirection()
            } else {
                showSensorNotEnabledToast(listOf("Magnetometro"))
            }
        }

        orientationButton.setOnClickListener {
            if (orientationButton.isEnabled) {
                getOrientation()
            } else {
                showSensorNotEnabledToast(listOf("Accelerometro", "Magnetometro"))
            }
        }

        inclinationButton.setOnClickListener {
            if (inclinationButton.isEnabled) {
                getInclination()
            } else {
                showSensorNotEnabledToast(listOf("Accelerometro", "Magnetometro"))
            }
        }

        inPocketButton.setOnClickListener {
            if (inPocketButton.isEnabled) {
                checkInPocket()
            } else {
                showSensorNotEnabledToast(listOf("Sensore di prossimità", "Sensore di luminosità"))
            }
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

            // Aspetta che il server sia pronto
            val serverReady = waitForServerStart()
            if (!serverReady) {
                withContext(Main) {
                    connectionStatus.text = "✗ Server non disponibile dopo il timeout!"
                    connectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    progressBar.visibility = View.GONE

                    // Disabilita tutti i bottoni quando non connesso
                    val allButtons = listOf(magnitudeButton, northButton, orientationButton, inclinationButton, inPocketButton)
                    allButtons.forEach {
                        it.isEnabled = false
                        it.alpha = 0.5f
                    }

                    Toast.makeText(requireContext(), "Server non disponibile", Toast.LENGTH_LONG).show()
                }
                Log.e("SENSOR", "Server non disponibile dopo timeout")
                return
            }

            // Leggi le preferenze per l'hostname e porta
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val port = sharedPrefs.getString("server_port", "8080")?.toIntOrNull() ?: 8080
            val useLocalIp = sharedPrefs.getBoolean("use_local_ip", false)
            val customHostname = sharedPrefs.getString("server_hostname", "")

            val actualHostname = when {
                !useLocalIp -> "localhost"
                !customHostname.isNullOrBlank() -> customHostname
                else -> getLocalIpAddress() ?: "localhost"
            }

            val url = "http://$actualHostname:$port/smartphone"
            val td = wot.requestThingDescription(url)
            smartphoneThing = wot.consume(td)

            withContext(Main) {
                progressBar.visibility = View.GONE
                connectionStatus.text = "✓ Connesso al dispositivo"
                connectionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

                // Aggiorna gli stati dei bottoni dopo la connessione
                updateButtonStates()
            }

            Log.d("SENSOR", "Connesso a Smartphone Thing")

        } catch (e: Exception) {
            withContext(Main) {
                progressBar.visibility = View.GONE
                connectionStatus.text = "✗ Errore di connessione: ${e.message}"
                connectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))

                // Disabilita tutti i bottoni in caso di errore
                val allButtons = listOf(magnitudeButton, northButton, orientationButton, inclinationButton, inPocketButton)
                allButtons.forEach {
                    it.isEnabled = false
                    it.alpha = 0.5f
                }

                Toast.makeText(requireContext(), "Impossibile connettersi al dispositivo", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore connessione a Smartphone Thing: ", e)
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

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SENSOR", "Errore ottenimento IP locale: ", e)
        }
        return null
    }

    private fun getMagnitude() {

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
                        magnitudeStatusText.text = "Magnitudine: ${String.format("%.2f", magnitude)} m/s²"
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

                // Riabilita il pulsante solo se il sensore è ancora abilitato
                updateButtonStates()
            }

            Log.d("SENSOR", "getMagnitude invocata con successo!")

        } catch (e: Exception) {
            withContext(Main) {
                magnitudeStatusText.text = "Magnitudine: Errore di comunicazione"
                magnitudeStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))

                updateButtonStates()

                Toast.makeText(requireContext(), "Errore durante il calcolo della magnitudine", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore invocando getMagnitude: ", e)
        }
    }

    private fun getNorthDirection() {
        northStatusText.text = "Direzione Nord: Calcolo in corso..."
        northStatusText.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))

        coroutineScope.launch { invokeGetNorthDirection() }
    }

    private suspend fun invokeGetNorthDirection() {
        try {
            val result = smartphoneThing?.invokeAction("getNorthDirection")
            withContext(Main) {
                if (result != null) {
                    val value = result.doubleValue() ?: -1.0
                    if (value >= 0) {
                        northStatusText.text = "Direzione Nord: ${String.format("%.2f", value)}°"
                        northStatusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    } else {
                        northStatusText.text = "Direzione Nord: Errore nel calcolo"
                        northStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                } else {
                    northStatusText.text = "Direzione Nord: Risposta non valida"
                    northStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                updateButtonStates()
            }
        } catch (e: Exception) {
            withContext(Main) {
                northStatusText.text = "Direzione Nord: Errore di comunicazione"
                northStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                updateButtonStates()
                Toast.makeText(requireContext(), "Errore calcolo direzione nord", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore getNorthDirection: ", e)
        }
    }

    // 1) getOrientation
    private fun getOrientation() {
        orientationStatusText.text = "Orientamento: Calcolo in corso..."
        orientationStatusText.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))

        coroutineScope.launch { invokeGetOrientation() }
    }

    private suspend fun invokeGetOrientation() {
        try {
            val result = smartphoneThing?.invokeAction("getOrientation")
            withContext(Main) {
                if (result != null && result.isArray) {
                    val arr = result as ArrayNode
                    if (arr.size() >= 3) {
                        val azimuth = arr[0].floatValue()
                        val pitch = arr[1].floatValue()
                        val roll = arr[2].floatValue()
                        orientationStatusText.text = "Orientamento:\nAzimuth: ${"%.2f".format(azimuth)}°\nPitch: ${"%.2f".format(pitch)}°\nRoll: ${"%.2f".format(roll)}°"
                        orientationStatusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    } else {
                        orientationStatusText.text = "Orientamento: Dati insufficienti"
                        orientationStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                } else {
                    orientationStatusText.text = "Orientamento: Risposta non valida"
                    orientationStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                updateButtonStates()
            }
        } catch (e: Exception) {
            withContext(Main) {
                orientationStatusText.text = "Orientamento: Errore di comunicazione"
                orientationStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                updateButtonStates()
                Toast.makeText(requireContext(), "Errore calcolo orientamento", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore getOrientation: ", e)
        }
    }

    // 2) getInclination
    private fun getInclination() {
        inclinationStatusText.text = "Inclinazione: Calcolo in corso..."
        inclinationStatusText.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))

        coroutineScope.launch { invokeGetInclination() }
    }

    private suspend fun invokeGetInclination() {
        try {
            val result = smartphoneThing?.invokeAction("getInclination")
            withContext(Main) {
                if (result != null) {
                    val value = result.doubleValue() ?: -1.0
                    if (value >= 0) {
                        inclinationStatusText.text = "Inclinazione: ${"%.2f".format(value)}°"
                        inclinationStatusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    } else {
                        inclinationStatusText.text = "Inclinazione: Errore nel calcolo"
                        inclinationStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                } else {
                    inclinationStatusText.text = "Inclinazione: Risposta non valida"
                    inclinationStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                updateButtonStates()
            }
        } catch (e: Exception) {
            withContext(Main) {
                inclinationStatusText.text = "Inclinazione: Errore di comunicazione"
                inclinationStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                updateButtonStates()
                Toast.makeText(requireContext(), "Errore calcolo inclinazione", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore getInclination: ", e)
        }
    }

    // 3) checkInPocket
    private fun checkInPocket() {
        inPocketStatusText.text = "Verifica in tasca: In corso..."
        inPocketStatusText.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))

        coroutineScope.launch { invokeCheckInPocket() }
    }

    private suspend fun invokeCheckInPocket() {
        try {
            val result = smartphoneThing?.invokeAction("checkInPocket")
            withContext(Main) {
                if (result != null) {
                    val inPocket = result.booleanValue() ?: false
                    if (inPocket) {
                        inPocketStatusText.text = "Dispositivo probabilmente in tasca"
                        inPocketStatusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    } else {
                        inPocketStatusText.text = "Dispositivo probabilmente NON in tasca"
                        inPocketStatusText.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                    }
                } else {
                    inPocketStatusText.text = "Verifica in tasca: Risposta non valida"
                    inPocketStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                updateButtonStates()
            }
        } catch (e: Exception) {
            withContext(Main) {
                inPocketStatusText.text = "Verifica in tasca: Errore di comunicazione"
                inPocketStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                updateButtonStates()
                Toast.makeText(requireContext(), "Errore verifica tasca", Toast.LENGTH_LONG).show()
            }
            Log.e("SENSOR", "Errore checkInPocket: ", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireActivity().unregisterReceiver(preferencesReceiver)
        } catch (e: Exception) {
            Log.w("SENSOR_ACTIONS", "Errore durante la deregistrazione del receiver: $e")
        }
        coroutineScope.cancel()
    }
}