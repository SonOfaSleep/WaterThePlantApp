package com.sonofasleep.watertheplantapp.utilities

import android.net.Uri
import android.util.Log
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import java.io.File
import java.net.URI

object FileManager {

    fun deleteImageFile(uri: Uri?) {
        if (uri == null) return

        val imageFile = File(URI.create(uri.toString()))
        if (!imageFile.exists()) return

        try {
            imageFile.delete()
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "${e.message}")
        }
    }
}