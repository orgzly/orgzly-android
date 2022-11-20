package com.orgzly.android.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.orgzly.BuildConfig
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.util.LogUtils

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, intent, savedInstanceState)

        startActivity(Intent(this, MainActivity::class.java))
        // startActivity(Intent(this, IntroActivity::class.java))

        finish()
    }

    companion object {
        private val TAG = LauncherActivity::class.java.name
    }
}