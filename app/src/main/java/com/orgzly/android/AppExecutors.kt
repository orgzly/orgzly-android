package com.orgzly.android

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper

import java.util.concurrent.Executor
import java.util.concurrent.Executors

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AppExecutors(
        private val diskIO: Executor,
        private val networkIO: Executor,
        private val mainThread: Executor
) {

    @Inject
    constructor() : this(
            // TODO: For Espresso - register an idling resource instead
            AsyncTask.THREAD_POOL_EXECUTOR,
            // Executors.newSingleThreadExecutor(),
            Executors.newFixedThreadPool(3),
            MainThreadExecutor()
    )

    fun diskIO(): Executor {
        return diskIO
    }

    fun networkIO(): Executor {
        return networkIO
    }

    fun mainThread(): Executor {
        return mainThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }
}
