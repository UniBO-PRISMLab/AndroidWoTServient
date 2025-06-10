package com.example.testserver

import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.thing.schema.WoTConsumedThing
import java.io.File
import java.io.FileOutputStream

class PicAudioActivity : AppCompatActivity() {
    private lateinit var wot: Wot
    private var smartphoneThing: WoTConsumedThing? = null

    private lateinit var photoImageView: ImageView
    private lateinit var audioTextView: TextView
    private lateinit var takePhotoButton: Button
    private lateinit var startRecordingButton: Button

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val jsonNodeFactory = JsonNodeFactory.instance

    // Per i permessi
    private companion object {
        const val CAMERA_PERMISSION_CODE = 1001
        const val AUDIO_PERMISSION_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pic_audio)

        photoImageView = findViewById(R.id.photoImageView)
        audioTextView = findViewById(R.id.audioTextView)
        takePhotoButton = findViewById(R.id.takePhotoButton)
        startRecordingButton = findViewById(R.id.startRecordingButton)
        MediaUtils.setCurrentActivity(this)

        wot = WoTClientHolder.wot!!
        coroutineScope.launch {
            connectToThing()
        }

        takePhotoButton.setOnClickListener {
            if (checkCameraPermission()) {
                executeTakePhoto()
            } else {
                requestCameraPermission()
            }
        }

        startRecordingButton.setOnClickListener {
            if (checkAudioPermission()) {
                executeStartRecording()
            } else {
                requestAudioPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MediaUtils.setCurrentActivity(this)
    }

    override fun onPause() {
        super.onPause()
        MediaUtils.setCurrentActivity(null)
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            AUDIO_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MEDIA", "Permesso camera concesso")
                    executeTakePhoto()
                } else {
                    Log.d("MEDIA", "Permesso camera negato!")
                    Toast.makeText(this, "Permesso camera necessario per fare foto", Toast.LENGTH_SHORT).show()
                }
            }
            AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MEDIA", "Permesso audio concesso")
                    executeStartRecording()
                } else {
                    Log.d("MEDIA", "Permesso audio negato!")
                    Toast.makeText(this, "Permesso audio necessario per registrare", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MediaUtils.REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                // L'immagine è stata scattata!
                val photoFile = File(this.externalCacheDir, "photo.jpg")
                if (photoFile.exists()) {
                    coroutineScope.launch {
                        try {
                            val bytes = photoFile.readBytes()
                            val base64Photo = Base64.encodeToString(bytes, Base64.DEFAULT)
                            val inputJsonNode = jsonNodeFactory.textNode(base64Photo)
                            smartphoneThing?.invokeAction("updatePhoto", inputJsonNode)
                            Log.d("MEDIA", "Invocata updatePhoto")

                            // Devi aggiornare manualmente la property
                            val updatedPhotoJsonNode = smartphoneThing?.readProperty("photo")
                            val updatedPhotoBase64 = updatedPhotoJsonNode?.value()?.asText()

                            if (updatedPhotoBase64 != null && updatedPhotoBase64.isNotEmpty()) {
                                val bytes = Base64.decode(updatedPhotoBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                photoImageView.setImageBitmap(bitmap)
                                Log.d("MEDIA", "Foto aggiornata in ImageView con 'photo' property")
                            }
                        } catch (e: Exception) {
                            Log.e("MEDIA", "Errore processando foto scattata!")
                        }
                    }
                } else {
                    Log.e("MEDIA", "File foto non trovato: ")
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("MEDIA", "Scatto foto annullato dall'utente")
            }
        }
    }

    private suspend fun connectToThing() {
        try {
            // TODO Controlla url corretto
            val url = "http://localhost:8080/smartphone"
            val td = wot.requestThingDescription(url)
            smartphoneThing = wot.consume(td)
            Log.d("MEDIA", "Connesso a Smartphone Thing")
        } catch (e: Exception) {
            Log.e("MEDIA", "Errore connessione a Smartphone Thing: ", e)
        }
    }

    private fun executeTakePhoto() {
        coroutineScope.launch {
            invokeTakePhoto()
        }
    }

    private fun executeStartRecording() {
        coroutineScope.launch {
            invokeStartRecording()
            delay(5000)
            readAudioProperty()
        }
    }

    private suspend fun invokeTakePhoto() {
        try {
            smartphoneThing?.invokeAction("takePhoto")
            Log.d("MEDIA", "takePhoto invocata")
        } catch (e: Exception) {
            Log.e("MEDIA", "Errore invocazione takePhoto: ", e)
        }
    }

    private suspend fun readPhotoProperty() {
        try {
            val base64Photo = smartphoneThing?.readProperty("photo")?.value()?.asText()
            if (base64Photo != null) {
                val bytes = Base64.decode(base64Photo, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                photoImageView.setImageBitmap(bitmap)
                Log.d("MEDIA", "Foto aggiornata!")
            } else {
                Log.d("MEDIA", "Nessuna foto disponibile")
            }
        } catch (e: Exception) {
            Log.e("MEDIA", "Errore leggendo foto: ", e)
        }
    }

    private suspend fun invokeStartRecording() {
        try {
            smartphoneThing?.invokeAction("startRecording")
            Log.d("MEDIA", "Invocato startRecording")
        } catch (e: Exception) {
            Log.e("MEDIA", "Errore invocando startRecording: ", e)
        }
    }

    private suspend fun readAudioProperty() {
        try {
            val base64Audio = smartphoneThing?.readProperty("audio")?.value()?.asText()
            if (base64Audio != null) {
                audioTextView.text = "Audio base64 length: ${base64Audio.length}"
                Log.d("MEDIA", "Audio aggiornato")
            } else {
                audioTextView.text = "Nessun audio disponibile!"
            }
        } catch (e: Exception) {
            Log.e("MEDIA", "Errore leggendo audio: ", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaUtils.setCurrentActivity(null)
        coroutineScope.cancel()
    }
}