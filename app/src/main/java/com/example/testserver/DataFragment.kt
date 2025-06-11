package com.example.testserver

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
            // TODO; VEDERE SE SERVER RUNNING
            val intent = Intent(requireContext(), PicAudioActivity::class.java)
            startActivity(intent)
        }
        sensorButton.setOnClickListener {
            val intent = Intent(requireContext(), SensorDataActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}