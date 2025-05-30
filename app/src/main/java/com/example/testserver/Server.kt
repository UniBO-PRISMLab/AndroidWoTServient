package com.example.testserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.reflection.ExposedThingBuilder
import org.eclipse.thingweb.thing.schema.WoTExposedThing
import java.io.File

class Server(
    private val wot: Wot,
    private val servient: Servient,
    private val context: Context
) {
    var photoThing: PhotoThing? = null
    var audioThing: AudioThing? = null

    suspend fun start(): List<WoTExposedThing> {
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val enableLightSensor = sharedPrefs.getBoolean("enable_light_sensor", true)
        val enablePressureSensor = sharedPrefs.getBoolean("enable_pressure_sensor", true)
        val enableMagnetometer = sharedPrefs.getBoolean("enable_magnetometer", true)
        val exposedThings = mutableListOf<WoTExposedThing>()

        // Counter
        val counterThing = CounterThing()
        val exposedCounter = ExposedThingBuilder.createExposedThing(wot, counterThing, CounterThing::class)
        if(exposedCounter != null) {
            servient.addThing(exposedCounter)
            servient.expose(exposedCounter.getThingDescription().id)
            exposedThings.add(exposedCounter)
        }

        // Light Sensor
        if(enableLightSensor) {
            val lightSensorThing = LightSensorThing(context)
            val exposedLightSensor = ExposedThingBuilder.createExposedThing(
                wot,
                lightSensorThing,
                LightSensorThing::class
            )
            if (exposedLightSensor != null) {
                servient.addThing(exposedLightSensor)
                servient.expose(exposedLightSensor.getThingDescription().id)
                exposedThings.add(exposedLightSensor)
            }
        }

        // Pressure Sensor -- NON ESISTE SUL DISPOSITIVO -- RIMUOVERE
        if(enablePressureSensor) {
            val pressureSensorThing = PressureSensorThing(context)
            val exposedPressureSensor = ExposedThingBuilder.createExposedThing(
                wot,
                pressureSensorThing,
                PressureSensorThing::class
            )
            if (exposedPressureSensor != null) {
                servient.addThing(exposedPressureSensor)
                servient.expose(exposedPressureSensor.getThingDescription().id)
                exposedThings.add(exposedPressureSensor)
            }
        }

        // Magnetometer
        if(enableMagnetometer) {
            val magnetometerThing = MagnetometerThing(context)
            val exposedMagnetometer = ExposedThingBuilder.createExposedThing(
                wot,
                magnetometerThing,
                MagnetometerThing::class
            )
            if (exposedMagnetometer != null) {
                servient.addThing(exposedMagnetometer)
                servient.expose(exposedMagnetometer.getThingDescription().id)
                exposedThings.add(exposedMagnetometer)
            }
        }

        // Sensori dinamici
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val availableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        // tranne quelli che ho già creato
        val excludedTypes = setOf(Sensor.TYPE_LIGHT, Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_PRESSURE)

        for (sensor in availableSensors) {
            if (sensor.type in excludedTypes) continue

            val prefKey = "enable_sensor_${sensor.type}"
            if(!sharedPrefs.getBoolean(prefKey, true)) continue

            val type = sensor.type
            val name = sensor.name
            val sensorThing = GenericSensorThing(context, type, name)
            val exposedThing = ExposedThingBuilder.createExposedThing(wot, sensorThing, GenericSensorThing::class)
            if (exposedThing != null) {
                val td = exposedThing.getThingDescription()
                td.id = "sensor-${sanitizeSensorName(name)}"
                td.title = name
                td.description = "Thing for sensor type: $type"

                servient.addThing(exposedThing)
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

    private fun sanitizeSensorName(name: String): String {
        return name.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
    }
}