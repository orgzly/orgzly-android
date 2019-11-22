package com.orgzly.android.usecase

import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.reminders.ReminderService
import com.orgzly.android.sync.AutoSync
import com.orgzly.android.util.LogUtils
import com.orgzly.android.widgets.ListWidgetProvider
import javax.inject.Inject


object UseCaseRunner {
    private val TAG = UseCaseRunner::class.java.name

    // FIXME: Make UserActionRunner a class
    class Factory {
        @Inject
        lateinit var autoSync: AutoSync

        @Inject
        lateinit var dataRepository: DataRepository

        init {
            App.appComponent.inject(this)
        }
    }

    @JvmStatic
    fun run(action: UseCase): UseCaseResult {
        val startedAt = System.currentTimeMillis()

        val factory = Factory()

        val result = action.run(factory.dataRepository)

        when (result.triggersSync) {
            UseCase.SYNC_DATA_MODIFIED -> factory.autoSync.trigger(AutoSync.Type.DATA_MODIFIED)
            UseCase.SYNC_NOTE_CREATED -> factory.autoSync.trigger(AutoSync.Type.NOTE_CREATED)
        }

        if (result.modifiesLocalData) {
            ReminderService.notifyForDataChanged(App.getAppContext())
            ListWidgetProvider.notifyDataChanged(App.getAppContext())
        }

        if (result.modifiesListWidget) {
            ListWidgetProvider.update(App.getAppContext())
        }

        if (BuildConfig.LOG_DEBUG) {
            val ms = System.currentTimeMillis() - startedAt
            LogUtils.d(TAG, "Finished ${action.javaClass.simpleName} in ${ms}ms")
        }

        return result
    }

    @JvmStatic
    fun enqueue(action: UseCase) {
        val context = App.getAppContext()
        UseCaseService.enqueueWork(context, action.toIntent())
    }
}