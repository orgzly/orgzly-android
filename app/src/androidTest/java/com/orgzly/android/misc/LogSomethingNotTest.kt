package com.orgzly.android.misc

import android.os.Environment
import android.util.Log
import com.orgzly.android.OrgzlyTest
import org.junit.Test

class LogSomethingNotTest : OrgzlyTest() {
    @Test
    fun testLink() {
        Log.i(
            "XXX", String.format(
                """
                    Environment.getExternalStorageDirectory: %s
                    context.filesDir: %s
                    context.getExternalFilesDir(null): %s
                    context.getExternalFilesDir(DOWNLOADS): %s""".trimIndent(),
                Environment.getExternalStorageDirectory(),
                context.filesDir,
                context.getExternalFilesDir(null),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            )
        );
    }
}
