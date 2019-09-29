package com.orgzly.android.query

import com.orgzly.org.datetime.OrgInterval

/**
 * [OrgInterval] with support for "none", "today" (0d), "tomorrow" (1d), "yesterday" (-1d).
 **/
class QueryInterval(val none: Boolean = false, val now: Boolean = false) : OrgInterval() {
    override fun toString(): String {
        return when {
            none -> "none"
            now -> "now"
            unit == Unit.DAY && value ==  0 -> "today"
            unit == Unit.DAY && value ==  1 -> "tomorrow"
            unit == Unit.DAY && value == -1 -> "yesterday"
            else -> super.toString()
        }
    }

    companion object {
        private val REGEX = Regex("^([-+]?\\d+)([hdwmy])$")

        fun parse(str: String): QueryInterval? = when (str.toLowerCase()) {
            "none", "no" -> {
                QueryInterval(none = true)
            }

            "now" -> {
                QueryInterval(now = true)
            }

            "today", "tod" -> {
                val interval = QueryInterval()
                interval.setValue(0)
                interval.setUnit(Unit.DAY)
                interval
            }

            "tomorrow", "tmrw", "tom" -> {
                val interval = QueryInterval()
                interval.setValue(1)
                interval.setUnit(Unit.DAY)
                interval
            }

            "yesterday" -> {
                val interval = QueryInterval()
                interval.setValue(-1)
                interval.setUnit(Unit.DAY)
                interval
            }

            else -> {
                val m = REGEX.find(str)

                if (m != null) {
                    val interval = QueryInterval()
                    interval.setValue(m.groupValues[1])
                    interval.setUnit(m.groupValues[2])
                    interval

                } else {
                    null
                }
            }
        }
    }
}