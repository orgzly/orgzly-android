package com.orgzly.android.query

import com.orgzly.org.datetime.OrgInterval
import java.util.regex.Pattern

/**
 * [OrgInterval] with support for "none", "today" (0d) and "tomorrow" (1d).
 **/
class QueryInterval(var none: Boolean = false) : OrgInterval() {
    override fun toString(): String {
        if (none) {
            return "none"

        } else if (unit == OrgInterval.Unit.DAY && value == 0) {
            return "today"

        } else if (unit == OrgInterval.Unit.DAY && value == 1) {
            return "tomorrow"
        }

        return super.toString()
    }

    companion object {
        private val PATTERN = Pattern.compile("^(\\d+)([hdwmy])$")

        fun parse(str: String): QueryInterval? = when (str.toLowerCase()) {
            "none", "no" -> {
                QueryInterval(none = true)
            }

            "today", "tod" -> {
                val interval = QueryInterval()
                interval.setValue(0)
                interval.setUnit(OrgInterval.Unit.DAY)
                interval            }

            "tomorrow", "tmrw", "tom" -> {
                val interval = QueryInterval()
                interval.setValue(1)
                interval.setUnit(OrgInterval.Unit.DAY)
                interval
            }

            else -> {
                val m = PATTERN.matcher(str)

                if (m.find()) {
                    val interval = QueryInterval()
                    interval.setValue(m.group(1))
                    interval.setUnit(m.group(2))
                    interval

                } else {
                    null
                }
            }
        }
    }
}