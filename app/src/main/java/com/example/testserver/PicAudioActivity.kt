package com.example.testserver

import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PicAudioActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var imageUri: Uri? = null
    private var audioPath: String? = null
    private var mediaRecorder: MediaRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pic_audio)

        imageView = findViewById(R.id.imageView)

        val takePictureButton = findViewById<Button>(R.id.takePictureButton)
        val recordAudioButton = findViewById<Button>(R.id.recordAudioButton)
        val stopRecordingButton = findViewById<Button>(R.id.stopRecordingButton)
        val sendButton = findViewById<Button>(R.id.sendButton)

        takePictureButton.setOnClickListener { openCamera() }
        recordAudioButton.setOnClickListener { startRecording() }
        stopRecordingButton.setOnClickListener { stopRecording() }
        sendButton.setOnClickListener { sendMedia() }
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

    // TODO: CAMBIARE SALVATAGGIO FILE/INVIARE DIRETTAMENTE
    private fun startRecording() {
        try {
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
    }

    private fun sendMedia() {
        //TODO: usa WoT per inviare
        Toast.makeText(this, "Invio Media (TODO)", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }
}