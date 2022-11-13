package com.orgzly.android.data.logs

import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.db.entity.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseAppLogsRepository @Inject constructor(db: OrgzlyDatabase) : AppLogsRepository {
    private val dbAppLog = db.appLog()

    override fun log(type: String, str: String) {
        val entry = AppLog(0, System.currentTimeMillis(), type, str)
        dbAppLog.insert(entry)
    }

    override fun getFlow(type: String): Flow<List<LogEntry>> {
        return dbAppLog.getFlow(type).map { logEntries ->
            logEntries.map { entry ->
                LogEntry(entry.timestamp, entry.name, entry.message)
            }
        }.flowOn(Dispatchers.IO)
    }
}