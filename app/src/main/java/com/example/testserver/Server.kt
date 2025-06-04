package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.util.Log
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.reflection.ExposedThingBuilder
import org.eclipse.thingweb.thing.schema.InteractionInput
import org.eclipse.thingweb.thing.schema.WoTExposedThing
import java.io.File
import java.util.concurrent.CountDownLatch

class Server(
    private val wot: Wot,
    private val servient: Servient,
    private val context: Context
) {
    var photoThing: PhotoThing? = null
    var audioThing: AudioThing? = null
    private val jsonNodeFactory = JsonNodeFactory.instance
    private val activeThings = mutableMapOf<String, WoTExposedThing>()

    suspend fun start(): List<WoTExposedThing> {
        val exposedThings = mutableListOf<WoTExposedThing>()
        updateExposedThings().forEach {
            exposedThings.add(it)
        }

        // Aggiungi Thing multimediali una sola volta
        // Photo Thing
        val photoFile = File(context.externalCacheDir, "photo.jpg")
        photoThing = PhotoThing(photoFile)
        val exposedPhoto =
            ExposedThingBuilder.createExposedThing(wot, photoThing!!, PhotoThing::class)
        if (exposedPhoto != null) {
            servient.addThing(exposedPhoto)
            servient.expose(exposedPhoto.getThingDescription().id)
            exposedThings.add(exposedPhoto)
            MediaThings.photoThing = photoThing
        }

        // Audio Record Thing
        val audioFile = File(context.externalCacheDir, "audio.3gp")
        audioThing = AudioThing(audioFile)
        val exposedAudio =
            ExposedThingBuilder.createExposedThing(wot, audioThing!!, AudioThing::class)
        if (exposedAudio != null) {
            servient.addThing(exposedAudio)
            servient.expose(exposedAudio.getThingDescription().id)
            exposedThings.add(exposedAudio)
            MediaThings.audioThing = audioThing
        }

        return exposedThings
    }

    suspend fun updateExposedThings(): List<WoTExposedThing> {
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val availableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        val wantedThingIds = mutableSetOf<String>()
        val newlyAdded = mutableListOf<WoTExposedThing>()

        for (sensor in availableSensors) {
            val prefKey = "share_sensor_${sensor.name}"
            val shouldShare = sharedPrefs.getBoolean(prefKey, false)
            if (!shouldShare) continue

            Log.d("SERVER_PREF", "Checking $prefKey = $shouldShare")
            val type = sensor.type
            val name = sensor.name
            val thingId = sanitizeSensorName(name, type)
            wantedThingIds.add(thingId)

            if (thingId in activeThings) continue

            val thing = wot.produce {
                id = thingId
                title = name
                description = "Thing for sensor $name, type: $type"

                numberProperty("value") {
                    title = "Sensor value"
                    readOnly = true
                    observable = true
                    unit = getSensorUnit(type)
                }
            }.apply {
                val sensorType = type
                setPropertyReadHandler("value") { input ->
                    try {
                        val sensorValue = readSensorValue(context, sensorType)
                        val jsonNode = jsonNodeFactory.numberNode(sensorValue)
                        InteractionInput.Value(jsonNode)
                    } catch (e: Exception) {
                        val errorNode = jsonNodeFactory.numberNode(-1f)
                        InteractionInput.Value(errorNode)
                    }
                }
            }


            servient.addThing(thing)
            servient.expose(thingId)
            activeThings[thingId] = thing
            newlyAdded.add(thing)
            Log.d("SERVER_UPDATE", "Added Thing: $thingId")
        }

        val toRemove = activeThings.keys - wantedThingIds
        for (thingId in toRemove) {
            try {
                servient.destroy(thingId)
                activeThings.remove(thingId)
                Log.d("SERVER_UPDATE", "Removed Thing: $thingId")
            } catch (e: Exception) {
                Log.e("SERVER_UPDATE", "Errore rimozione $thingId", e)
            }
        }

        return newlyAdded
    }

    private fun sanitizeSensorName(name: String, type: Int): String {
        val sanitizedName = name.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
        return "$sanitizedName-$type"
    }

    fun readSensorValue(context: Context, sensorType: Int): Float {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return -1f

        val latch = CountDownLatch(1)
        var value = -1f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    value = event.values[0]
                    latch.countDown()
                    sensorManager.unregisterListener(this)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        try {
            latch.await(200, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        sensorManager.unregisterListener(listener)
        return value
    }

    fun getSensorUnit(type: Int): String? = when (type) {
        Sensor.TYPE_LIGHT -> "lux"
        Sensor.TYPE_PRESSURE -> "hPa"
        Sensor.TYPE_ACCELEROMETER -> "m/s^2"
        Sensor.TYPE_MAGNETIC_FIELD -> "μT"
        Sensor.TYPE_PROXIMITY -> "cm"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
        else -> null
    }


}
