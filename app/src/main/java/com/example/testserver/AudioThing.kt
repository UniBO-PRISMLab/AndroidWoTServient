package com.example.testserver

import android.util.Base64
import org.eclipse.thingweb.reflection.annotations.Action
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing
import java.io.File

@Thing(
    id = "audio-thing",
    title = "Audio Thing",
    description = "A Thing that exposes the latest recorded audio"
)
class AudioThing(private val audioFile: File) {
    @Property(
        title = "Latest Audio",
        description = "Base64-encoded audio recording",
        readOnly = true
    )
    var latestAudio: String = ""
        private set

    init {
        loadAudio()
    }

    @Action(
        title = "Refresh Audio",
        description = "Reload latest audio from disk"
    )
    fun refresh() {
        loadAudio()
    }

    private fun loadAudio() {
        if (audioFile.exists()) {
            val bytes = audioFile.readBytes()
            latestAudio = Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else {
            latestAudio = ""
        }
    }
}