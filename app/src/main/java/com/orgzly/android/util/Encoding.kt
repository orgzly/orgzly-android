package com.orgzly.android.util

import android.util.Log
import com.orgzly.BuildConfig
import java.io.File
import java.io.FileInputStream

data class Encoding(val used: String, val detected: String?, val selected: String?) {
    companion object {
        /**
         * Determine encoding to use -- detect or force it.
         */
        fun detect(pathname: String, selectedEncoding: String?): Encoding {
            val usedEncoding: String
            var detectedEncoding: String? = null

            if (selectedEncoding == null) {
                val startedAt = System.currentTimeMillis()

                detectedEncoding = EncodingDetect.getInstance(FileInputStream(File(pathname))).encoding

                if (BuildConfig.LOG_DEBUG) {
                    val ms = System.currentTimeMillis() - startedAt
                    LogUtils.d(TAG, "Detected $pathname encoding in ${ms}ms: $detectedEncoding")
                }

                /* Can't detect encoding - use default. */
                if (detectedEncoding == null) {
                    usedEncoding = DEFAULT_ENCODING
                    Log.w(TAG, "Encoding for $pathname not be detected, using $DEFAULT_ENCODING")
                } else {
                    usedEncoding = detectedEncoding
                }

            } else {
                usedEncoding = selectedEncoding
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Using selected encoding: $usedEncoding")
            }

            return Encoding(usedEncoding, detectedEncoding, selectedEncoding)
        }

        private const val DEFAULT_ENCODING = "UTF-8"

        private val TAG = Encoding::class.java.name
    }
}