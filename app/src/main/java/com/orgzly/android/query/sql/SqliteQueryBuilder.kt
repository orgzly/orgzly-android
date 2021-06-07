package com.orgzly.android.query.sql

import android.content.Context
import android.database.DatabaseUtils
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.query.*
import com.orgzly.org.datetime.OrgInterval
import java.util.*


class SqliteQueryBuilder(val context: Context) {
    private var where: String = ""
    private val arguments: MutableList<String> = ArrayList()

    private var having: String = ""

    private var order: String = ""

    private var hasScheduledCondition = false
    private var hasDeadlineCondition = false
    private var hasCreatedCondition = false

    fun build(query: Query): SqlQuery {
        hasScheduledCondition = false
        hasDeadlineCondition = false
        hasCreatedCondition = false

        where = toString(query.condition)

        order = buildOrderBy(query.sortOrders)

        return SqlQuery(where, arguments, having, order)
    }

    private fun buildOrderBy(sortOrders: List<SortOrder>): String {
        val o = ArrayList<String>()

        if (sortOrders.isEmpty()) { // Use default sort order
            o.add("book_name")

            /* Priority or default priority. */
            o.add("COALESCE(priority, '" + AppPreferences.defaultPriority(context) + "')")
            o.add("priority IS NULL")

            if (hasScheduledCondition) {
                o.add("scheduled_time_timestamp IS NULL")
                o.add("scheduled_time_start_of_day")
                o.add("scheduled_time_hour IS NULL")
                o.add("scheduled_time_timestamp")
            }

            if (hasDeadlineCondition) {
                o.add("deadline_time_timestamp IS NULL")
                o.add("deadline_time_start_of_day")
                o.add("deadline_time_hour IS NULL")
                o.add("deadline_time_timestamp")
            }

            if (hasCreatedCondition) {
                o.add("created_at DESC")
            }

        } else {
            sortOrders.forEach { order ->
                when (order) {
                    is SortOrder.Book ->
                        o.add("book_name" + if (order.desc) " DESC" else "")

                    is SortOrder.Title ->
                        o.add("title" + if (order.desc) " DESC" else "")

                    is SortOrder.Scheduled -> {
                        o.add("scheduled_time_timestamp IS NULL")

                        if (order.desc) {
                            o.add("scheduled_time_start_of_day DESC")
                            o.add("scheduled_time_hour IS NOT NULL")
                            o.add("scheduled_time_timestamp DESC")

                        } else {
                            o.add("scheduled_time_start_of_day")
                            o.add("scheduled_time_hour IS NULL")
                            o.add("scheduled_time_timestamp")
                        }
                    }

                    is SortOrder.Deadline -> {
                        o.add("deadline_time_timestamp IS NULL")

                        if (order.desc) {
                            o.add("deadline_time_start_of_day DESC")
                            o.add("deadline_time_hour IS NOT NULL")
                            o.add("deadline_time_timestamp DESC")

                        } else {
                            o.add("deadline_time_start_of_day")
                            o.add("deadline_time_hour IS NULL")
                            o.add("deadline_time_timestamp")
                        }
                    }

                    is SortOrder.Event -> {
                        o.add("event_timestamp IS NULL")

                        if (order.desc) {
                            o.add("MAX(event_start_of_day) DESC")
                            o.add("MAX(event_hour) IS NOT NULL")
                            o.add("MAX(event_timestamp) DESC")

                        } else {
                            o.add("MIN(event_start_of_day)")
                            o.add("MIN(event_hour) IS NULL")
                            o.add("MIN(event_timestamp)")
                        }
                    }

                    is SortOrder.Created -> {
                        o.add("created_at IS NULL")

                        if (order.desc) {
                            o.add("created_at DESC")

                        } else {
                            o.add("created_at")
                        }
                    }

                    is SortOrder.Closed -> {
                        o.add("closed_time_timestamp IS NULL")

                        if (order.desc) {
                            o.add("closed_time_start_of_day DESC")
                            o.add("closed_time_hour IS NOT NULL")
                            o.add("closed_time_timestamp DESC")

                        } else {
                            o.add("closed_time_start_of_day")
                            o.add("closed_time_hour IS NULL")
                            o.add("closed_time_timestamp")
                        }
                    }

                    is SortOrder.Priority -> {
                        o.add("COALESCE(priority, '" + AppPreferences.defaultPriority(context) + "')" + if (order.desc) " DESC" else "")
                        o.add("priority" + if (order.desc) " IS NOT NULL" else " IS NULL")
                    }

                    is SortOrder.State -> {
                        val states = AppPreferences.todoKeywordsSet(context)
                                .union(AppPreferences.doneKeywordsSet(context))

                        if (states.isNotEmpty()) {
                            val statesInOrder = if (order.desc) states.reversed() else states

                            o.add(statesInOrder.foldIndexed("CASE state") { i, str, state ->
                                "$str WHEN ${DatabaseUtils.sqlEscapeString(state)} THEN $i"
                            } + " ELSE ${states.size} END")
                        }
                    }

                    is SortOrder.Position -> {
                        o.add("lft" + if (order.desc) " DESC" else "")
                    }
                }
            }
        }

        /* Always sort by position last. */
        o.add("lft")

        return o.joinToString(", ")
    }

    private fun joinConditions(members: List<Condition>, operator: String): String {
        return members.joinToString(prefix = "(", separator = " $operator ", postfix = ")") {
            toString(it)
        }
    }

    private fun toString(expr: Condition?): String {
        fun not(not: Boolean, selection: String): String = if (not) "NOT($selection)" else selection

        return when (expr) {
            is Condition.InBook -> {
                arguments.add(expr.name)
                not(expr.not, "book_name = ?")
            }

            is Condition.HasState -> {
                arguments.add(expr.state.toUpperCase())
                not(expr.not, "COALESCE(state, '') = ?")
            }

            is Condition.HasStateType -> {
                when (expr.type) {
                    StateType.TODO -> {
                        val states = AppPreferences.todoKeywordsSet(context)
                        arguments.addAll(states)
                        not(expr.not, "COALESCE(state, '') IN (" + Collections.nCopies(states.size, "?").joinToString() + ")")
                    }
                    StateType.DONE -> {
                        val states = AppPreferences.doneKeywordsSet(context)
                        arguments.addAll(states)
                        not(expr.not, "COALESCE(state, '') IN (" + Collections.nCopies(states.size, "?").joinToString() + ")")

                    }
                    StateType.NONE -> not(expr.not, "COALESCE(state, '') = ''")
                }
            }

            is Condition.HasPriority -> {
                arguments.add(AppPreferences.defaultPriority(context))
                arguments.add(expr.priority)
                not(expr.not, "LOWER(COALESCE(NULLIF(priority, ''), ?)) = ?")
            }

            is Condition.HasSetPriority -> {
                arguments.add(expr.priority)
                not(expr.not, "LOWER(COALESCE(priority, '')) = ?")
            }

            is Condition.HasTag -> {
                repeat(2) { arguments.add("%${expr.tag}%") }
                not(expr.not, "(COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)")
            }

            is Condition.HasOwnTag -> {
                arguments.add("%${expr.tag}%")
                not(expr.not, "(COALESCE(tags, '') LIKE ?)")
            }

            is Condition.Event -> {
                toInterval("event_timestamp", null, expr.interval, expr.relation)
            }

            is Condition.Scheduled -> {
                hasScheduledCondition = true
                toInterval("scheduled_time_timestamp", "scheduled_is_active", expr.interval, expr.relation)
            }

            is Condition.Deadline -> {
                hasDeadlineCondition = true
                toInterval("deadline_time_timestamp", "deadline_is_active", expr.interval, expr.relation)
            }

            is Condition.Created -> {
                hasCreatedCondition = true
                toInterval("created_at", null, expr.interval, expr.relation)
            }

            is Condition.Closed -> {
                toInterval("closed_time_timestamp", null, expr.interval, expr.relation)
            }

            is Condition.HasText -> {
                repeat(3) { arguments.add("%${expr.text}%") }
                "(title LIKE ? OR content LIKE ? OR tags LIKE ?)"
            }

            is Condition.Or -> joinConditions(expr.operands, "OR")
            is Condition.And -> joinConditions(expr.operands, "AND")

            null -> "" // No conditions
        }
    }

    private fun toInterval(column: String, isActiveColumn: String?, interval: QueryInterval, relation: Relation): String {
        if (interval.none) {
            return "$column IS NULL"
        }

        val (field, value) = getFieldAndValueFromInterval(interval)

        val timeFromNow = TimeUtils.timeFromNow(field, value)
        val timeFromNowPlusOne = TimeUtils.timeFromNow(field, value, true)


        val cond = when (relation) {
            Relation.EQ -> "$timeFromNow <= $column AND $column < $timeFromNowPlusOne"
            Relation.NE -> "$column < $timeFromNow AND $timeFromNowPlusOne <= $column"
            Relation.LT -> "$column < $timeFromNow"
            Relation.LE -> "$column < $timeFromNowPlusOne"
            Relation.GT -> "$timeFromNowPlusOne <= $column"
            Relation.GE -> "$timeFromNow <= $column"
        }

        val activeOnly = if (isActiveColumn != null) {
            "$isActiveColumn = 1 AND "
        } else {
            ""
        }

        return "($activeOnly$column != 0 AND $cond)"
    }

    /*
     * TODO: Clean this up.
     * There's no need to depend on Org-supported units.
     * Remove OrgInterval dependency from QueryInterval.
     */
    private fun getFieldAndValueFromInterval(interval: QueryInterval): Pair<Int, Int> {
        return if (interval.now) {
            Pair(Calendar.MILLISECOND, 0)

        } else {
            val unit = when (interval.unit) {
                OrgInterval.Unit.HOUR -> Calendar.HOUR_OF_DAY
                OrgInterval.Unit.DAY -> Calendar.DAY_OF_MONTH
                OrgInterval.Unit.WEEK -> Calendar.WEEK_OF_YEAR
                OrgInterval.Unit.MONTH -> Calendar.MONTH
                OrgInterval.Unit.YEAR -> Calendar.YEAR

                null -> throw IllegalArgumentException("Interval unit not set")
            }

            val value = interval.value

            Pair(unit, value)
        }
    }
}
