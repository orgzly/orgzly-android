package com.orgzly.android.util

import android.content.Intent
import android.os.Bundle
import android.util.Log

import com.orgzly.android.db.entity.Note

/**
 * Methods for logging debugging information.
 *
 * They are slow and should *never* be called by production code.
 * Call them under "if (BuildConfig.LOG_DEBUG)" so they are removed by ProGuard.
 */
object LogUtils {
    private const val LOGCAT_BUFFER_SIZE = 1024

    /**
     * Returns the last method found in stack trace (before this class).
     */
    private val callerMethodName: String
        get() {
            var lastMethod = "UNKNOWN-METHOD"

            val ste = Thread.currentThread().stackTrace

            for (i in ste.indices.reversed()) {
                if (ste[i].toString().contains(LogUtils::class.java.name)) {
                    return lastMethod
                }

                lastMethod = ste[i].methodName
            }

            return lastMethod
        }

    /**
     * Logs caller's method name followed by specified parameters.
     */
    @JvmStatic
    fun d(tag: String, vararg args: Any?) {
        val s = StringBuilder()

        for (i in args.indices) {
            when (val arg = args[i]) {
                is Array<*> -> appendArray(s, arg)
                is Intent -> appendIntent(s, arg)
                is Bundle -> appendBundle(s, arg)
                else -> s.append(arg)
            }

            if (i < args.size - 1) {
                s.append(" --- ")
            }
        }

        /* Prefix with caller's method name. */
        if (s.isNotEmpty()) {
            s.insert(0, ": ")
        }
        s.insert(0, callerMethodName)

        /* Prefix with thread id and name. */
        val thread = Thread.currentThread()
        s.insert(0, thread.id.toString() + "#" + thread.name + ": ")

        doLog(tag, s.toString())
    }

    private fun appendArray(s: StringBuilder, array: Array<*>) {
        s.append(array.joinToString("|"))
    }

    private fun appendIntent(s: StringBuilder, intent: Intent) {
        s.append(intent).append(" ")
        appendBundle(s, intent.extras)
    }

    private fun appendBundle(s: StringBuilder, bundle: Bundle?) {
        if (bundle != null) {
            s.append(bundle.keySet().joinToString(", ", "Bundle[", "]") { key ->
                key + ":" + bundle.get(key)
            })
        }
    }

    private fun notes(notes: List<Note>): String {
        return notes.joinToString("\n") { note ->
            "id:%-3d  parent:%-3d  descendantsCount:%-3d %-3d %-3d  %s %s".format(
                    note.id,
                    note.position.parentId,
                    note.position.descendantsCount,
                    note.position.lft,
                    note.position.rgt,
                    "*".repeat(note.position.level),
                    note.title
            )
        }
    }

    /**
     * Logs in chunks, due to logcat limit.
     */
    private fun doLog(tag: String, s: String) {
        val length = s.length

        var i = 0
        while (i < length) {
            if (i + LOGCAT_BUFFER_SIZE < length) {
                Log.d(tag, s.substring(i, i + LOGCAT_BUFFER_SIZE))
            } else {
                Log.d(tag, s.substring(i, length))
            }
            i += LOGCAT_BUFFER_SIZE
        }
    }
}
