package com.orgzly.android.query

object QueryUtils {
    @JvmStatic
    fun extractFirstBookNameFromQuery(condition: Condition?): String? {
        if (condition is Condition.And) {
            val (operands) = condition

            for (innerCondition in operands) {
                val result = extractFirstBookNameFromQuery(innerCondition)

                if (result != null) {
                    return result
                }
            }

        } else if (condition is Condition.Or) {
            val (operands) = condition

            for (innerCondition in operands) {
                val result = extractFirstBookNameFromQuery(innerCondition)

                if (result != null) {
                    return result
                }
            }

        } else if (condition is Condition.InBook) {
            val (name, not) = condition

            if (!not) {
                return name
            }
        }

        return null
    }
}
