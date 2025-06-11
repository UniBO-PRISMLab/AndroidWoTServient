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
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
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
import kotlinx.coroutines.withContext
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.thing.schema.WoTConsumedThing
import java.io.File
import java.io.FileOutputStream

class PicAudioActivity : BaseActivity() {
    private lateinit var wot: Wot
    private var smartphoneThing: WoTConsumedThing? = null

    private lateinit var photoImageView: ImageView
    private lateinit var audioTextView: TextView
    private lateinit var takePhotoButton: Button
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var playAudioButton: Button

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val jsonNodeFactory = JsonNodeFactory.instance

    // Per i permessi
    private companion object {
        const val CAMERA_PERMISSION_CODE = 1001
        const val AUDIO_PERMISSION_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_with_nav)
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
        val layout = layoutInflater.inflate(R.layout.activity_pic_audio, contentFrame, false)
        contentFrame.addView(layout)
        setupBottomNavigation(R.id.nav_data)
        initializeViews(layout)

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
                invokeStartRecordingAction()
            } else {
                requestAudioPermission()
            }
        }

        stopRecordingButton.setOnClickListener {
            invokeStopRecordingActionAndRead()
        }

        playAudioButton.setOnClickListener {
            playRecordedAudio()
        }

        stopRecordingButton.isEnabled = false
        playAudioButton.isEnabled = false
    }

    private fun initializeViews(rootView: View) {
        photoImageView = rootView.findViewById(R.id.photoImageView)
        audioTextView = rootView.findViewById(R.id.audioTextView)
        takePhotoButton = rootView.findViewById(R.id.takePhotoButton)
        startRecordingButton = rootView.findViewById(R.id.startRecordingButton)
        stopRecordingButton = rootView.findViewById(R.id.stopRecordingButton)
        playAudioButton = rootView.findViewById(R.id.playRecordingButton)
    }

    override fun onResume() {
        super.onResume()
        MediaUtils.setCurrentActivity(this)
    }

    override fun onPause() {
        super.onPause()
        MediaUtils.setCurrentActivity(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaUtils.setCurrentActivity(null)
        coroutineScope.cancel()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAudioPermission(): Boolean {
        val recordAudioGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val writeStoreGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        return recordAudioGranted && writeStoreGranted
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun requestAudioPermission() {
        val permissions = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
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
                    invokeStartRecordingAction()
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

    private suspend fun invokeTakePhoto() {
        try {
            smartphoneThing?.invokeAction("takePhoto")
            Log.d("MEDIA", "takePhoto invocata")
        } catch (e: Exception) {
            Log.e("MEDIA", "Errore invocazione takePhoto: ", e)
        }
    }

    private fun invokeStartRecordingAction() {
        coroutineScope.launch {
            try {
                smartphoneThing?.invokeAction("startRecording", jsonNodeFactory.nullNode())
                Log.d("AUDIO", "Invocata startRecording")
                startRecordingButton.isEnabled = false
                stopRecordingButton.isEnabled = true
                playAudioButton.isEnabled = false
            } catch (e: Exception) {
                Log.e("AUDIO", "Errore invocando startRecording: ", e)
            }
        }
    }

    private fun invokeStopRecordingActionAndRead() {
        coroutineScope.launch {
            try {
                smartphoneThing?.invokeAction("stopRecording", jsonNodeFactory.nullNode())
                Log.d("AUDIO", "Invocato stopRecording")
                val updatedAudioJsonNode = smartphoneThing?.readProperty("audio")
                val updatedAudioBase64 = updatedAudioJsonNode?.value()?.asText()
                if (updatedAudioBase64 != null && updatedAudioBase64.isNotEmpty()) {
                    playAudioButton.isEnabled = true
                    Log.d("AUDIO", "Proprietà letta?")
                } else {
                    Log.e("AUDIO", "Proprietà audio vuota o nulla")
                }
                startRecordingButton.isEnabled = true
                stopRecordingButton.isEnabled = false
            } catch (e: Exception) {
                Log.e("AUDIO", "Errore fermando registrazione o leggendo audio")
            }
        }
    }

    private fun playRecordedAudio() {
        coroutineScope.launch {
            try {
                val base64Audio = smartphoneThing?.readProperty("audio")?.value()?.asText()
                if (base64Audio != null && base64Audio.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        MediaUtils.playAudio(base64Audio, applicationContext)
                    }
                } else {
                    Toast.makeText(this@PicAudioActivity, "Nessun audio da riprodurre!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AUDIO", "Errore durante riproduzione: ", e)
            }
        }
    }
}