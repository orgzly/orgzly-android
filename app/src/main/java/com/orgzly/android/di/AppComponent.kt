package com.orgzly.android.di

import com.orgzly.android.App
import com.orgzly.android.ChooserShareTargetService
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.data.DataRepository
import com.orgzly.android.di.module.AndroidModule
import com.orgzly.android.di.module.ApplicationModule
import com.orgzly.android.di.module.DataModule
import com.orgzly.android.di.module.DatabaseModule
import com.orgzly.android.widgets.ListWidgetService
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidInjectionModule::class,
    ApplicationModule::class,
    AndroidModule::class,
    DatabaseModule::class,
    DataModule::class
])
interface AppComponent : AndroidInjector<App> {

    fun inject(dataRepository: DataRepository)

    fun inject(factory: UseCaseRunner.Factory)

    fun inject(widgetViewsFactory: ListWidgetService.ListWidgetViewsFactory)

    fun inject(chooserShareTargetService: ChooserShareTargetService)

//    @Component.Builder
//    abstract class Builder : AndroidInjector.Builder<App>()

//    @Component.Builder
//    interface Builder {
//        @BindsInstance
//        fun application(application: Application): Builder

//        @BindsInstance
//        fun appModule(appModule: AppModule): Builder

//        fun build(): AppComponent
//    }
}