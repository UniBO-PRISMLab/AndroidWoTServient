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

        /* TODO: Prova con un sensore
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val sensorThing = GenericSensorThing(context, Sensor.TYPE_LIGHT, "Light Sensor")
        val exposedThing = ExposedThingBuilder.createExposedThing(wot, sensorThing,
            GenericSensorThing::class)
        if (exposedThing != null) {
            val td = exposedThing.getThingDescription()
            td.id = "sensor-light"
            td.title = "Light Sensor"
            td.description = "Thing for light sensor"
            servient.addThing(exposedThing)
            servient.expose(td.id)
            println("Exposed light sensor Thing")
            exposedThings.add(exposedThing)
        }*/

        for (sensor in availableSensors) {
            val prefKey = "share_sensor_${sensor.name}"
            if (!sharedPrefs.getBoolean(prefKey, true)) continue

            val type = sensor.type
            val name = sensor.name
            val thingId = "sensor-${sanitizeSensorName(name)}"
            val sensorThing = GenericSensorThing(context, type, name)
            val exposedThing =
                ExposedThingBuilder.createExposedThing(wot, sensorThing, GenericSensorThing::class)
            if (exposedThing != null) {
                val ipAddress = getLocalIpAddress()
                val port = 8080
                val td = exposedThing.getThingDescription()
                td.forms = td.forms.map { form ->
                    form.copy(href = form.href.replace("localhost", "$ipAddress:$port"))
                }.toMutableList()

                for (prop in td.properties.values) {
                    prop.forms = prop.forms.map { form ->
                        form.copy(href = form.href.replace("localhost", "$ipAddress:$port"))
                    }.toMutableList()
                }
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

    private fun sanitizeSensorName(name: String): String {
        return name.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
    }
}

private fun getLocalIpAddress(): String {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (intf in interfaces) {
        val addrs = intf.inetAddresses
        for (addr in addrs) {
            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                return addr.hostAddress ?: "127.0.0.1"
            }
        }
    }
    return "127.0.0.1"
}