package com.orgzly.android.di.module

import android.app.Application
import android.content.res.Resources
import com.orgzly.android.LocalStorage
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.DbRepoBookRepository
import com.orgzly.android.data.logs.AppLogsRepository
import com.orgzly.android.data.logs.DatabaseAppLogsRepository
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.usecase.UseCaseRunner
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal open class DataModule {
    @Provides
    @Singleton
    internal fun providesDataRepository(
            app: Application,
            database: OrgzlyDatabase,
            repoFactory: RepoFactory,
            resources: Resources,
            localStorage: LocalStorage
    ): DataRepository {
        return DataRepository(app, database, repoFactory, resources, localStorage)
    }

    @Provides
    @Singleton
    internal fun providesRepoFactory(app: Application, dbRepoBookRepository: DbRepoBookRepository): RepoFactory {
        return RepoFactory(app, dbRepoBookRepository)
    }

    @Provides
    @Singleton
    internal fun providesUserActionRunnerFactory(): UseCaseRunner.Factory {
        return UseCaseRunner.Factory()
    }

    @Provides
    @Singleton
    internal fun providesLogsRepository(database: OrgzlyDatabase): AppLogsRepository {
        return DatabaseAppLogsRepository(database)
    }


//    @Provides
//    fun providesLocalStorage(context: Application): LocalStorage {
//        return LocalStorage(context.applicationContext)
//    }
}