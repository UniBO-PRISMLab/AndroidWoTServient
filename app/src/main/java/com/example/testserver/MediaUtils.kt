package com.example.testserver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

// TODO: togliere setCurrentActivity e cambiare..

object MediaUtils {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var currentActivityRef: WeakReference<Activity>? = null
    private var mediaPlayer: MediaPlayer? = null

    const val REQUEST_IMAGE_CAPTURE = 1

    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = activity?.let { WeakReference(it) }
    }

    fun takePhoto(context: Context) {
        try {
            val activity = currentActivityRef?.get()
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

    fun startAudioRecording(context: Context) {
        if (isRecording) {
            Log.w("AUDIO", "Registrazione già in corso!")
            return
        }

        audioFilePath = "${context.externalCacheDir?.absolutePath}/recorded_audio.3gp"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                isRecording = true
                Log.d("MediaUtils", "Registrazione audio iniziata")
            } catch (e: kotlinx.io.IOException) {
                Log.e("AUDIO", "prepare() failed: ", e)
                isRecording = false
            }
        }
    }

    fun stopAudioRecording(): String? {
        if (!isRecording) {
            Log.w("AUDIO", "Nessuna registrazione da stoppare!")
            return null
        }
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            Log.d("AUDIO", "Registrazione stoppata")
            val audioFile = audioFilePath?.let { File(it) }
            if (audioFile != null && audioFile.exists()) {
                val bytes = audioFile.readBytes()
                val base64Audio = Base64.encodeToString(bytes, Base64.DEFAULT)
                Log.d("AUDIO", "Audio convertito in base64")
                return base64Audio
            } else {
                Log.e("AUDIO", "File audio non trovato!: ")
                return null
            }
        } catch (e: Exception) {
            Log.e("AUDIO", "Errore durante stop o lettura audio: ", e)
            return null
        }
    }

    fun playAudio(base64Audio: String, context: Context) {
        if (base64Audio.isEmpty()) {
            Log.w("AUDIO", "Nessun audio da riprodurre")
            return
        }

        mediaPlayer?.release()
        mediaPlayer = null

        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val tmpAudioFile = File(context.cacheDir, "tmp_audio.3gp")
            FileOutputStream(tmpAudioFile).use { fos ->
                fos.write(audioBytes)
            }
            Log.d("AUDIO", "Audio decodificato e salvato!")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tmpAudioFile.absolutePath)
                prepare()
                start()
                Log.d("AUDIO", "Riproduzione audio avviata!")
                setOnCompletionListener { mp ->
                    mp.release() // Rilascia le risorse una volta completata la riproduzione
                    tmpAudioFile.delete() // Cancella il file temporaneo
                    mediaPlayer = null
                    Log.d("AUDIO", "Riproduzione audio completata e MediaPlayer rilasciato.")
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("AUDIO", "Errore MediaPlayer: what=$what, extra=$extra")
                    mp.release()
                    tmpAudioFile.delete()
                    mediaPlayer = null
                    currentActivityRef?.get()?.runOnUiThread {
                        android.widget.Toast.makeText(context, "Errore durante la riproduzione dell'audio.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    true // Indica che l'errore è stato gestito
                }
            }
        } catch (e: Exception) {
            Log.e("AUDIO", "Errore durante decodfica o riproduzione: ", e)
        }
    }
}