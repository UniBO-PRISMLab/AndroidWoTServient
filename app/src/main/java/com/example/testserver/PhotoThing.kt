package com.example.testserver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.eclipse.thingweb.reflection.annotations.Action
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing
import java.io.ByteArrayOutputStream
import java.io.File

@Thing(
    id = "photo-thing",
    title = "Photo Thing",
    description = "A Thing that exposs the latest photo taken"
)
class PhotoThing(private val photoFile: File) {
    @Property(
        title = "Latest Photo",
        description = "Base64-encoded image from the last photo",
        readOnly = true
    )
    var latestPhoto: String = ""
        private set

    init {
        loadPhoto()
    }

    @Action(
        title = "Refresh Photo",
        description = "Reload latest photo from disk"
    )
    fun refresh() {
        loadPhoto()
    }

    private fun loadPhoto() {
        if(photoFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            latestPhoto = bitmapToBase64(bitmap)
        } else {
            latestPhoto = ""
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}