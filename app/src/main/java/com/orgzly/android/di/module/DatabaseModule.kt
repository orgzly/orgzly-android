package com.orgzly.android.di.module

import android.content.Context
import com.orgzly.android.db.OrgzlyDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class DatabaseModule(val testing: Boolean = false) {
    @Provides
    @Singleton
    internal fun provideDatabase(context: Context): OrgzlyDatabase {
        return if (testing) {
            OrgzlyDatabase.forFile(context, OrgzlyDatabase.NAME_FOR_TESTS)
            // return OrgzlyDatabase.forMemory(context)
        } else {
            OrgzlyDatabase.forFile(context, OrgzlyDatabase.NAME)
        }
    }
}