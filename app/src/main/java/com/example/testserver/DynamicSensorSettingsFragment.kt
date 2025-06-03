package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat

// Mostra elenco di switch -- ognuno permette di scegliere se condividere o no sensore -- viene salvata la preferenza usando come chiave "share_sensor_<nomesensore>"
class DynamicSensorSettingsFragment : PreferenceFragmentCompat() {
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
        }
    }
}