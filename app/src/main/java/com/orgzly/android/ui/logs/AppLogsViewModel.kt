package com.orgzly.android.ui.logs

import com.orgzly.android.data.logs.AppLogsRepository
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.util.LogMajorEvents
import kotlinx.coroutines.flow.map
import java.util.*

class AppLogsViewModel(appLogsRepository: AppLogsRepository) : CommonViewModel() {
    val logs = appLogsRepository.getFlow(LogMajorEvents.REMINDERS).map {
        it.map { logEntry ->
            val date = Date(logEntry.time)
            val type = logEntry.type
            val message = logEntry.message

            "$date $type $message"
        }
    }
}