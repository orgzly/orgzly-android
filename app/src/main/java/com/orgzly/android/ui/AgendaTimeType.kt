package com.orgzly.android.ui

data class AgendaTimeType(
        val timeType: TimeType,
        val isWarning: Boolean = false,
        val overdueDays: Int = 0)