package com.orgzly.android.util

import android.util.Log
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.prefs.AppPreferences
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class LogMajorEvents {
    companion object {

        const val REMINDERS = "reminders"

        fun isEnabled(): Boolean {
            return AppPreferences.logMajorEvents(App.getAppContext())
        }
    }

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
