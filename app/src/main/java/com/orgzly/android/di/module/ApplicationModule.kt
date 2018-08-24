package com.orgzly.android.di.module

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.orgzly.android.LocalStorage
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
internal class ApplicationModule(var application: Application) {
    @Provides
    @Singleton
    internal fun providesApplication(): Application {
        return application
    }

    @Provides
    @Singleton
    internal fun providesContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    internal fun providesResources(context: Context): Resources {
        return context.resources
    }

    @Provides
    @Singleton
    internal fun providesLocalStorage(context: Context): LocalStorage {
        return LocalStorage(context)
    }
}