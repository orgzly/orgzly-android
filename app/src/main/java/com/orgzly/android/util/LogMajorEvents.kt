package com.orgzly.android.util

import android.util.Log
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.prefs.AppPreferences
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object LogMajorEvents {
    const val REMINDERS = "reminders"

    private const val FILE_NAME = "debug.log"

    fun isEnabled(): Boolean {
        return AppPreferences.logMajorEvents(App.getAppContext())
    }

    @Synchronized
    fun log(name: String, str: String) {
        try {
            val file = getFile()

            rotateIfTooBig(file)

            // Append to file
            OutputStreamWriter(FileOutputStream(file, true)).use {
                val msg = "${DateTime.now()} $name $str\n"
                it.append(msg)
            }

            if (BuildConfig.LOG_DEBUG) {
                val tag = logTagWithName(name)
                LogUtils.d(tag, str)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Writing to a log file failed", e)
        }
    }

    private fun logTagWithName(name: String) = "com.orgzly-log-$name"

    fun readLogFile(): String {
        return try {
            getFile().readText()
        } catch (e: Exception) {
            val stackTrace = Log.getStackTraceString(e)
            "File couldn't be read\n$stackTrace"
        }
    }

    private fun getFile(): File {
        return File(App.getAppContext().filesDir, FILE_NAME)
    }

    // Limit *size*, keep *one* older log file. TODO: To preferences
    private const val MAX_SIZE = 1024 * 1024
    private fun rotateIfTooBig(file: File): File? {
        return if (file.length() > MAX_SIZE) {
            val rotatedFile = File(file.parentFile, "$FILE_NAME.1")
            file.renameTo(rotatedFile)
            rotatedFile
        } else {
            null
        }
    }

    private val TAG: String = LogMajorEvents::class.java.name

//    class VM : ViewModel() {
//        fun logCatOutput(name: String) = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
//            // Runtime.getRuntime().exec("logcat -c")
//            // val tag = tagWithName(name)
//            // Runtime.getRuntime().exec("logcat *:S $tag")
//            Runtime.getRuntime().exec("logcat")
//                .inputStream
//                .bufferedReader()
//                .useLines { lines ->
//                    lines.forEach { line ->
//                        if (line.contains("orgzly")) {
//                            emit(line)
//                        }
//                    }
//                }
//        }
//    }

//    fun readLogCatOutput(name: String): List<String> {
//        val output = mutableListOf<String>()
//        Runtime.getRuntime().exec("logcat -d")
//            .inputStream
//            .bufferedReader()
//            .useLines { lines ->
//                lines.forEach { line ->
//                    if (line.contains("orgzly")) {
//                        output.add(line)
//                    }
//                }
//            }
//        return output
//    }
}
