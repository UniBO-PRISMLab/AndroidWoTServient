package com.example.testserver

import android.content.Intent
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference.OnPreferenceChangeListener

// QUESTA PER LA PORTA
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Valida la porta inserita
        val portPref = findPreference<EditTextPreference>("server_port")
        portPref?.setOnBindEditTextListener { editText ->
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        portPref?.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val port = (newValue as? String)?.toIntOrNull()
            val valid = port != null && port in 1024..65535
            if (valid) {
                requireContext().sendBroadcast(
                    Intent("PREFERENCES_UPDATED").putExtra("update_type", "port")
                )
            }
            valid
        }
    }
}