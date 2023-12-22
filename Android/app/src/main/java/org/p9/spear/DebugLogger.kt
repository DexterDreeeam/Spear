package org.p9.spear

import android.content.Context
import java.io.File

class DebugLogger(private val context: Context) {

    companion object {
        const val logName = "log.txt"
    }

    fun append(log: String) {
        try {
            val stream = context.openFileOutput(logName, Context.MODE_APPEND)
            stream.write(log.toByteArray())
            stream.close()
        } catch (ex: Exception) {
            // ignore
        }
    }

    private fun clean() {
        val file = File(context.filesDir, logName)
        if (file.exists()) {
            file.delete()
        }
    }

    fun reset() {
        clean()
        try {
            val stream = context.openFileOutput(logName, Context.MODE_PRIVATE)
            stream.close()
        } catch (ex: Exception) {
            // ignore
        }
    }
}