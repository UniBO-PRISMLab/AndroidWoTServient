package com.example.testserver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Handler
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

class DataFragment : Fragment() {

    private var mediaButton: Button? = null
    private var sensorButton: Button? = null

    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "SERVICE_STATUS_CHANGED") {
                updateButtonStates()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data, container, false)
        mediaButton = view.findViewById<Button>(R.id.mediaButton)
        sensorButton = view.findViewById<Button>(R.id.sensorButton)

        mediaButton?.setOnClickListener {
            handleMediaButtonClick()
        }
        sensorButton?.setOnClickListener {
            handleSensorButtonClick()
        }

        // Inizializza i bottoni come disabilitati per sicurezza
        initializeButtonsAsDisabled()

        return view
    }

    override fun onResume() {
        super.onResume()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Context.RECEIVER_NOT_EXPORTED else 0
        requireContext().registerReceiver(
            serviceStatusReceiver,
            IntentFilter("SERVICE_STATUS_CHANGED"),
            flags
        )

        // Pulisci eventuali stati residui dalle SharedPreferences
        clearStaleServerStates()

        // Aspetta un attimo prima di aggiornare per dare tempo al receiver di essere pronto
        Handler(Looper.getMainLooper()).postDelayed ({
            if (isAdded) {
                updateButtonStates()
            }
        }, 100)

        // Backup: controlla di nuovo dopo un delay più lungo
        Handler(Looper.getMainLooper()).postDelayed ({
            if (isAdded) {
                updateButtonStates()
            }
        }, 500)
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(serviceStatusReceiver)
        } catch (e: Exception) {
            // Non fare nulla
        }
    }

    private fun initializeButtonsAsDisabled() {
        mediaButton?.isEnabled = false
        sensorButton?.isEnabled = false
        mediaButton?.text = "📱 Media (Verifica in corso...)"
        sensorButton?.text = "📊 Sensori (Verifica in corso...)"
        updateButtonAppearance(mediaButton, false, android.R.color.holo_blue_light)
        updateButtonAppearance(sensorButton, false, android.R.color.holo_green_light)
    }

    private fun handleMediaButtonClick() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val enableHttp = prefs.getBoolean("enable_http", true)
        val serverStatus = getServerStatus()
        when {
            !enableHttp -> {
                Toast.makeText(requireContext(), "HTTP deve essere abilitato!", Toast.LENGTH_LONG).show()
            }
            serverStatus == ServerStatus.RUNNING -> {
                val intent = Intent(requireContext(), PicAudioActivity::class.java)
                startActivity(intent)
            }
            serverStatus == ServerStatus.STARTING -> {
                Toast.makeText(requireContext(), "Server in avvio, attendere..", Toast.LENGTH_SHORT).show()
            }
            serverStatus == ServerStatus.STOPPED -> {
                Toast.makeText(requireContext(), "Avvia il Server!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSensorButtonClick() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val enableHttp = prefs.getBoolean("enable_http", true)
        val serverStatus = getServerStatus()

        when {
            !enableHttp -> {
                Toast.makeText(requireContext(), "Abilita HTTP", Toast.LENGTH_SHORT).show()
            }
            serverStatus == ServerStatus.RUNNING -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SensorDataFragment())
                    .addToBackStack(null)
                    .commit()
            }
            serverStatus == ServerStatus.STARTING -> {
                Toast.makeText(requireContext(), "Server in avvio, attendere..", Toast.LENGTH_SHORT).show()
            }
            serverStatus == ServerStatus.STOPPED -> {
                Toast.makeText(requireContext(), "Avvia il Server!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateButtonStates() {
        if (!isAdded) return // Controlla se il fragment è ancora attached

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val enableHttp = prefs.getBoolean("enable_http", true)
        val serverStatus = getServerStatus()
        val isEnabled = serverStatus == ServerStatus.RUNNING && enableHttp

        // Debug: aggiungi un log per vedere cosa succede
        println("DataFragment - Server status: $serverStatus, Buttons enabled: $isEnabled")

        mediaButton?.isEnabled = isEnabled
        sensorButton?.isEnabled = isEnabled

        // Aggiorna i colori e la trasparenza per rendere visibile lo stato
        updateButtonAppearance(mediaButton, isEnabled, android.R.color.holo_blue_light)
        updateButtonAppearance(sensorButton, isEnabled, android.R.color.holo_green_light)

        when {
            !enableHttp -> {
                mediaButton?.text = "📱 Media (HTTP Disabilitato)"
                sensorButton?.text = "📊 Sensori (HTTP Disabilitato)"
            }
            serverStatus == ServerStatus.STARTING -> {
                mediaButton?.text = "📱 Media (Avvio...)"
                sensorButton?.text = "📊 Sensori (Avvio...)"
            }
            serverStatus == ServerStatus.RUNNING -> {
                mediaButton?.text = "📱 Apri Media (Foto/Audio)"
                sensorButton?.text = "📊 Visualizza Dati Sensori"
            }
            serverStatus == ServerStatus.STOPPED -> {
                mediaButton?.text = "📱 Media (Server Spento)"
                sensorButton?.text = "📊 Sensori (Server Spento)"
            }
        }
    }

    private fun updateButtonAppearance(button: Button?, enabled: Boolean, enabledColorRes: Int) {
        button?.let {
            if (enabled) {
                it.setBackgroundColor(ContextCompat.getColor(requireContext(), enabledColorRes))
                it.alpha = 1.0f
            } else {
                // Colore grigio quando disabilitato
                it.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                it.alpha = 0.6f
            }
        }
    }

    private fun clearStaleServerStates() {
        // Pulisce eventuali stati residui che potrebbero causare problemi
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = prefs.edit()

        // Verifica se il servizio è effettivamente in esecuzione
        val isServiceRunning = isServiceRunning("com.example.testserver.WoTService") // Sostituisci con il nome del tuo servizio

        if (!isServiceRunning) {
            // Se il servizio non è in esecuzione, resetta gli stati
            editor.putBoolean("server_started", false)
            editor.putBoolean("server_starting", false)
            editor.apply()
            println("DataFragment - Servizio non in esecuzione, stati resettati")
        } else {
            println("DataFragment - Servizio trovato in esecuzione")
        }
    }

    private fun isServiceRunning(serviceName: String): Boolean {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.getRunningServices(Integer.MAX_VALUE).any { service ->
            service.service.className == serviceName
        }
    }

    private fun getServerStatus(): ServerStatus {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isStarted = prefs.getBoolean("server_started", false)
        val isStarting = prefs.getBoolean("server_starting", false)

        // Debug: mostra i valori delle SharedPreferences
        println("DataFragment - SharedPrefs: server_started=$isStarted, server_starting=$isStarting")

        return when {
            isStarted -> ServerStatus.RUNNING
            isStarting -> ServerStatus.STARTING
            else -> ServerStatus.STOPPED
        }
    }

    private enum class ServerStatus {
        RUNNING, STARTING, STOPPED
    }
}