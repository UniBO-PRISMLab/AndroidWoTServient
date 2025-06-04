package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.reflection.ExposedThingBuilder
import org.eclipse.thingweb.thing.schema.WoTExposedThing
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

class Server(
    private val wot: Wot,
    private val servient: Servient,
    private val context: Context
) {
    var photoThing: PhotoThing? = null
    var audioThing: AudioThing? = null

    suspend fun start(): List<WoTExposedThing> {
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val exposedThings = mutableListOf<WoTExposedThing>()

        // Sensori dinamici
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val availableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        val addedThingIds = mutableSetOf<String>()

        for (sensor in availableSensors) {
            val prefKey = "share_sensor_${sensor.name}"
            val shouldShare = sharedPrefs.getBoolean(prefKey, false)
            if (!shouldShare) continue

            Log.d("SERVER_PREF", "Checking $prefKey = $shouldShare")
            val type = sensor.type
            val name = sensor.name
            val thingId = "${sanitizeSensorName(name, type)}"

            // Salta se ID già aggiunto
            if (thingId in addedThingIds) continue
            addedThingIds.add(thingId)

            val sensorThing = GenericSensorThing(context, type, name)
            val exposedThing =
                ExposedThingBuilder.createExposedThing(wot, sensorThing, GenericSensorThing::class)
            if (exposedThing != null) {
                val td = exposedThing.getThingDescription()
                td.id = thingId
                td.title = name
                td.description = "Thing for sensor type: $type"

                servient.addThing(exposedThing)
                Log.d("DEBUG", "Exposing: $thingId from sensor: ${sensor.name}")
                servient.expose(td.id)
                exposedThings.add(exposedThing)
            }
        }

        // Photo Thing
        val photoFile = File(context.externalCacheDir, "photo.jpg")
        photoThing = PhotoThing(photoFile)
        val exposedPhoto = ExposedThingBuilder.createExposedThing(wot, photoThing!!, PhotoThing::class)
        if (exposedPhoto != null) {
            servient.addThing(exposedPhoto)
            servient.expose(exposedPhoto.getThingDescription().id)
            exposedThings.add(exposedPhoto)
            MediaThings.photoThing = photoThing
        }

        // Audio Record Thing
        val audioFile = File(context.externalCacheDir, "audio.3gp")
        audioThing = AudioThing(audioFile)
        val exposedAudio = ExposedThingBuilder.createExposedThing(wot, audioThing!!, AudioThing::class)
        if (exposedAudio != null) {
            servient.addThing(exposedAudio)
            servient.expose(exposedAudio.getThingDescription().id)
            exposedThings.add(exposedAudio)
            MediaThings.audioThing = audioThing
        }

        return exposedThings
    }

    private fun sanitizeSensorName(name: String, type: Int): String {
        val sanitizedName = name.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
        return "$sanitizedName-$type"
    }
}