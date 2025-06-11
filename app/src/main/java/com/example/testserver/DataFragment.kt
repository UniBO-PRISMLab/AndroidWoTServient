package com.example.testserver

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class DataFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data, container, false)
        val mediaButton = view.findViewById<Button>(R.id.mediaButton)
        val sensorButton = view.findViewById<Button>(R.id.sensorButton)
        mediaButton.setOnClickListener {
            if (isServerRunning()) {
                val intent = Intent(requireContext(), PicAudioActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Avvia il Server!", Toast.LENGTH_SHORT).show()
            }
        }
        sensorButton.setOnClickListener {
            if (isServerRunning()) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SensorDataFragment())
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(requireContext(), "Avvia il Server!", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun isServerRunning(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return prefs.getBoolean("server_started", false)
    }
}