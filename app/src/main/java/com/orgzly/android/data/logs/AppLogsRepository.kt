package com.orgzly.android.data.logs

import kotlinx.coroutines.flow.Flow

interface AppLogsRepository {
    fun log(type: String, str: String)

    fun getFlow(type: String): Flow<List<LogEntry>>
}