package com.example.testserver

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class SettingsMenuFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings_menu, container, false)
        val selectSensorsButton = view.findViewById<Button>(R.id.selectSensorsBtn)
        val serverSettingsButton = view.findViewById<Button>(R.id.serverSettingsBtn)

        selectSensorsButton.setOnClickListener {
            val intent = Intent(requireContext(), SelectSensorsActivity::class.java)
            startActivity(intent)
        }

        serverSettingsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}