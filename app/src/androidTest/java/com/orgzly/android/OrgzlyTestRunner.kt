package com.orgzly.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class OrgzlyTestRunner : AndroidJUnitRunner() {

    /**
     * Uses [AppUnderTest] instead of [App].
     */
    @Throws(InstantiationException::class, IllegalAccessException::class, ClassNotFoundException::class)
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, AppUnderTest::class.java.name, context)
    }
}