package com.example.testserver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException

object MediaUtils {
    private var recorder: MediaRecorder? = null
    private var currentActivity: Activity? = null

    const val REQUEST_IMAGE_CAPTURE = 1

    fun setCurrentActivity(activity: Activity?) {
        currentActivity = activity
    }

    fun takePhoto(context: Context) {
        try {
            val activity = currentActivity
            if (activity == null) {
                Log.e("MEDIAUTILS", "Nessuna activity registrata")
                return
            }
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile = File(context.externalCacheDir, "photo.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

            activity.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            Log.d("MEDIAUTILS", "Intent Fotocamera avviato")
        } catch (e: Exception) {
            Log.e("MediaUtils", "Errore scattando foto", e)
        }
    }

    fun recordAudio(context: Context) {
        stopRecording() // Stop eventuali registrazioni precedenti

        val audioFile = File(context.externalCacheDir, "audio.3gp")
        recorder = MediaRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
                Log.d("MediaUtils", "Registrazione audio iniziata")
            } catch (e: IOException) {
                Log.e("MediaUtils", "Errore preparando MediaRecorder", e)
                release()
            }
        }

        // Stop dopo 5 secondi (puoi cambiare la durata)
        android.os.Handler(context.mainLooper).postDelayed({
            stopRecording()
        }, 5000)
    }

    private fun stopRecording() {
        recorder?.apply {
            try {
                stop()
                release()
                Log.d("MediaUtils", "Registrazione audio terminata")
            } catch (e: Exception) {
                Log.e("MediaUtils", "Errore terminando la registrazione", e)
            }
        }
        recorder = null
    }
}