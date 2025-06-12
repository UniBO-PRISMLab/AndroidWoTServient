package com.example.testserver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Mostra elenco di switch -- ognuno permette di scegliere se condividere o no sensore -- viene salvata la preferenza usando come chiave "share_sensor_<nomesensore>"
class DynamicSensorSettingsFragment : PreferenceFragmentCompat() {

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null && key.startsWith("share_sensor_")) {
                requireContext().sendBroadcast(
                    Intent("PREFERENCES_UPDATED").putExtra("update_type", "sensors")
                )
                Log.d("DYNAMIC_PREF", "Broadcast inviato per chiave: $key")
            }
        }

    private var initialSensorPrefs: Map<String, Any?> = emptyMap()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        val screen: PreferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        for(sensor in sensors) {
            val key = "share_sensor_${sensor.name}"
            val pref = SwitchPreferenceCompat(context).apply {
                title = sensor.name
                summary = "Abilita/disabilita ${sensor.name}"
                this.key = key
                setDefaultValue(false)
            }
            screen.addPreference(pref)
            Log.d("SENSOR_PREFS", "Added preference key: share_sensor_${sensor.name}")
        }

        initialSensorPrefs = sharedPrefs.all.filterKeys { it.startsWith("share_sensor_") }
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onStop() {
        super.onStop()

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentSensorPrefs = sharedPrefs.all.filterKeys { it.startsWith("share_sensor_") }

        if (currentSensorPrefs != initialSensorPrefs) {
            Log.d("DYNAMIC_PREF", "Preferenze dei sensori cambiate, riavvio servizio...")

            // Imposta lo stato "starting" prima di riavviare
            sharedPrefs.edit()
                .putBoolean("server_starting", true)
                .putBoolean("server_started", false)
                .apply()

            // Notifica il cambio di stato
            requireContext().sendBroadcast(Intent("SERVICE_STATUS_CHANGED"))

            // Ferma il servizio in modo più controllato
            val stopIntent = Intent(requireContext(), WoTService::class.java).apply {
                action = "STOP_SERVICE"
            }
            requireContext().startService(stopIntent)

            // Avvia il servizio dopo un delay più lungo per permettere lo stop completo
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000) // Delay più lungo per permettere lo stop completo
                try {
                    val intent = Intent(requireContext(), WoTService::class.java)
                    ContextCompat.startForegroundService(requireContext(), intent)
                    Log.d("DYNAMIC_PREF", "Servizio riavviato")
                } catch (e: Exception) {
                    Log.e("DYNAMIC_PREF", "Errore nel riavvio del servizio: ", e)
                    // Ripristina lo stato in caso di errore
                    sharedPrefs.edit()
                        .putBoolean("server_starting", false)
                        .putBoolean("server_started", false)
                        .apply()
                    requireContext().sendBroadcast(Intent("SERVICE_STATUS_CHANGED"))
                }
            }
        }
    }
}