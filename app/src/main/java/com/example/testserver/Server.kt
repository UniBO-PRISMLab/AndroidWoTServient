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

            val sensorValuesCount = getSensorValuesCount(type)
            val units = getSensorUnits(type)
            val thing = wot.produce {
                id = thingId
                title = name
                description = "Thing for sensor $name, type: $type"

                if (sensorValuesCount == 1) {
                    numberProperty("value") {
                        title = "Sensor value"
                        readOnly = true
                        observable = true
                        unit = units.getOrNull(0)
                    }
                } else {
                    for (i in 0 until sensorValuesCount) {
                        val propName = listOf("x", "y", "z").getOrNull(i) ?: "v$i"
                        numberProperty(propName) {
                            title = "Component $propName"
                            readOnly = true
                            observable = true
                            unit = units.getOrNull(i)
                        }
                    }
                }
            }.apply {
                val sensorType = type
                if (sensorValuesCount == 1) {
                    setPropertyReadHandler("value") {
                        ServientStats.logRequest(thingId, "readProperty")
                        val v = readSensorValues(context, sensorType)
                        InteractionInput.Value(jsonNodeFactory.numberNode(v.getOrNull(0) ?: -1f))
                    }
                } else {
                    for (i in 0 until sensorValuesCount) {
                        val propName = listOf("x", "y", "z").getOrNull(i) ?: "v$i"
                        setPropertyReadHandler(propName) {
                            ServientStats.logRequest(thingId, "readProperty")
                            val v = readSensorValues(context, sensorType)
                            InteractionInput.Value(jsonNodeFactory.numberNode(v.getOrNull(i) ?: -1f))
                        }
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

    private fun getSensorValuesCount(sensorType: Int): Int = when (sensorType) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_MAGNETIC_FIELD -> 3

        Sensor.TYPE_ROTATION_VECTOR -> 4
        Sensor.TYPE_GAME_ROTATION_VECTOR -> 4
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> 5

        else -> 1
    }

    private fun sanitizeSensorName(name: String, type: Int): String {
        val sanitizedName = name.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
        return "$sanitizedName-$type"
    }

    fun readSensorValues(context: Context, sensorType: Int): FloatArray {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return floatArrayOf()

        val latch = CountDownLatch(1)
        var values: FloatArray = floatArrayOf()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    values = event.values.copyOf()
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
        return values
    }

    fun getSensorUnits(type: Int): List<String?> = when (type) {
        Sensor.TYPE_LIGHT -> listOf("lux")
        Sensor.TYPE_PRESSURE -> listOf("hPa")
        Sensor.TYPE_ACCELEROMETER -> listOf("m/s^2")
        Sensor.TYPE_MAGNETIC_FIELD -> listOf("μT")
        Sensor.TYPE_PROXIMITY -> listOf("cm")
        Sensor.TYPE_AMBIENT_TEMPERATURE -> listOf("°C")
        Sensor.TYPE_RELATIVE_HUMIDITY -> listOf("%")
        else -> listOf(null)
    }


}
