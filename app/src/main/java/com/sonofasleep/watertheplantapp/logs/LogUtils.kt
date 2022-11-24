package com.sonofasleep.watertheplantapp.logs

import android.content.Context
import android.icu.text.SimpleDateFormat
import com.sonofasleep.watertheplantapp.const.DEBUG_DIR
import java.io.File

class LogUtils(val context: Context) {

    fun saveLogToFile(input: String) {
        val fileName = "Debug_logs.txt"
        val outputDir = File(context.applicationContext.filesDir, DEBUG_DIR)
        if (!outputDir.exists()) {
            outputDir.mkdir()
        }
        val outputFile = File(outputDir, fileName)
        val currentTime = SimpleDateFormat.getDateTimeInstance().toString()

        try {
            outputFile.appendText("$currentTime - $input\n")
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }
}