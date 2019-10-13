package com.orgzly.android

import android.annotation.SuppressLint
import com.orgzly.android.di.DaggerAppComponent
import com.orgzly.android.di.module.ApplicationModule
import com.orgzly.android.di.module.DatabaseModule
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import android.os.StrictMode


@SuppressLint("Registered")
class AppUnderTest : App() {
    override fun onCreate() {
        super.onCreate()

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

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        appComponent = DaggerAppComponent
                .builder()
                .applicationModule(ApplicationModule(this))
                .databaseModule(DatabaseModule(testing = true))
                .build()

        return appComponent
    }
}