package com.example.testserver

import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PicAudioActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var imageUri: Uri? = null
    private var audioPath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private val CAMERA_REQUEST_CODE = 100
    private val AUDIO_REQUEST_CODE = 101
    private lateinit var recordAudioButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var playAudioButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pic_audio)

        imageView = findViewById(R.id.imageView)

        val takePictureButton = findViewById<Button>(R.id.takePictureButton)
        recordAudioButton = findViewById(R.id.recordAudioButton)
        stopRecordingButton = findViewById(R.id.stopRecordingButton)
        playAudioButton = findViewById(R.id.playAudioButton)
        val sendButton = findViewById<Button>(R.id.sendButton)
        playAudioButton.isEnabled = false
        stopRecordingButton.isEnabled = false

        takePictureButton.setOnClickListener { checkCameraPermission() }
        recordAudioButton.setOnClickListener { checkAudioPermission() }
        stopRecordingButton.setOnClickListener { stopRecording() }
        sendButton.setOnClickListener { sendMedia() }
    }

    private fun checkCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? android.graphics.Bitmap
            imageView.setImageBitmap(bitmap)
            // Salvare bitmap su file per inviarlo
        }
    }

    private fun checkAudioPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), AUDIO_REQUEST_CODE)
        } else {
            startRecording()
        }
    }

    // TODO: CAMBIARE SALVATAGGIO FILE/INVIARE DIRETTAMENTE
    private fun startRecording() {
        try {
            recordAudioButton.isEnabled = false
            playAudioButton.isEnabled = false
            val filePath = "${externalCacheDir?.absolutePath}/audio.3gp"
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(filePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
            audioPath = filePath
            Toast.makeText(this, "Registrazione Avviata", Toast.LENGTH_SHORT).show()
            stopRecordingButton.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(this, "Errore nella Registrazione", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        Toast.makeText(this, "Registrazione Terminata", Toast.LENGTH_SHORT).show()
        recordAudioButton.isEnabled = true
        stopRecordingButton.isEnabled = false
        playAudioButton.isEnabled = true
    }

    private fun sendMedia() {
        //TODO: usa WoT per inviare
        Toast.makeText(this, "Invio Media (TODO)", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
                }
            }
            AUDIO_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    Toast.makeText(this, "Permesso microfono negato", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }
}