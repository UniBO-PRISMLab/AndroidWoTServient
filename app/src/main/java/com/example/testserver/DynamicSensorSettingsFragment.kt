package com.example.testserver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat

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

    private fun getFriendlyName(sensor: Sensor): String {
        return when (sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometro"
            Sensor.TYPE_LIGHT -> "Sensore di luminosità"
            Sensor.TYPE_GYROSCOPE -> "Giroscopio"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometro"
            Sensor.TYPE_PRESSURE -> "Barometro"
            Sensor.TYPE_PROXIMITY -> "Sensore di prossimità"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Sensore temperatura ambiente"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Sensore umidità"
            Sensor.TYPE_GRAVITY -> "Sensore gravità"
            else -> sensor.name
        }
    }

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        val screen: PreferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val allowedSensorTypes = listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_GRAVITY
        )

        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            .filter {sensor -> sensor.type in allowedSensorTypes}
        val filteredSensors = filterByNonWakeupSensors(sensors)

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        for(sensor in filteredSensors) {
            val key = "share_sensor_${sensor.name}"
            val friendlyName = getFriendlyName(sensor)

            val pref = SwitchPreferenceCompat(context).apply {
                title = friendlyName
                summary = "Abilita/disabilita $friendlyName"
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

        val context = context ?: return
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentSensorPrefs = sharedPrefs.all.filterKeys { it.startsWith("share_sensor_") }

        if (currentSensorPrefs != initialSensorPrefs) {
            Log.d("DYNAMIC_PREF", "Preferenze dei sensori cambiate, riavvio servizio..")
            // Riavvo dal Service
            context.sendBroadcast(
                Intent("PREFERENCES_UPDATED").putExtra("update_type", "sensors_restart")
            )

            Log.d("DYNAMIC_PREF", "Broadcast per restart Service inviato!")
        }
    }
}