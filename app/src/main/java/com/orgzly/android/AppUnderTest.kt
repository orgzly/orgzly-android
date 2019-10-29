package com.orgzly.android

import android.annotation.SuppressLint
import com.orgzly.android.di.DaggerAppComponent
import com.orgzly.android.di.module.ApplicationModule
import com.orgzly.android.di.module.DatabaseModule
import android.os.StrictMode


@SuppressLint("Registered")
class AppUnderTest : App() {
    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent
                .builder()
                .applicationModule(ApplicationModule(this))
                .databaseModule(DatabaseModule(testing = true))
                .build()

        if (false) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())
        }

        if (false) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    // .permitNonSdkApiUsage()
                    .penaltyLog()
                    // .penaltyDeath()
                    .build())
        }
    }
}