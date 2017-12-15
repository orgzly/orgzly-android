package com.orgzly.android.query.dotted

import android.content.Context
import com.orgzly.android.query.*
import com.orgzly.android.query.QuotedStringTokenizer

open class DottedQueryBuilder(val context: Context) : UserQueryBuilder {
    private var string: String = ""

    override fun getString(): String = string

    override fun build(query: Query) {
        val list = mutableListOf<String>()

        append(list, query.condition)
        append(list, query.sortOrders)
        append(list, query.options)

        string = list.joinToString(" ")
    }

    private fun append(list: MutableList<String>, condition: Condition) {
        val str = toString(condition, true)

        if (str.isNotEmpty()) {
            list.add(str)
        }
    }

    private fun toString(expr: Condition, isOuter: Boolean = false): String {
        fun dot(not: Boolean): String = if (not) "." else ""

        return when (expr) {
            is Condition.InBook -> "${dot(expr.not)}b.${quote(expr.name)}"

            is Condition.HasState -> "${dot(expr.not)}i.${expr.state}"

            is Condition.HasStateType -> {
                when (expr.type) {
                    StateType.DONE -> "${dot(expr.not)}it.done"
                    StateType.TODO -> "${dot(expr.not)}it.todo"
                    StateType.NONE -> "${dot(expr.not)}it.none"
                }
            }

            is Condition.HasPriority -> "${dot(expr.not)}p.${expr.priority}"
            is Condition.HasSetPriority -> "${dot(expr.not)}ps.${expr.priority}"

            is Condition.HasTag -> "${dot(expr.not)}t.${expr.tag}"
            is Condition.HasOwnTag -> "tn.${expr.tag}"

            is Condition.Scheduled -> {
                val rel = expr.relation.toString().toLowerCase()
                val relString = if (rel == "le") "" else ".$rel"
                "s$relString.${expr.interval}"
            }

            is Condition.Deadline -> {
                val rel = expr.relation.toString().toLowerCase()
                val relString = if (rel == "le") "" else ".$rel"
                "d$relString.${expr.interval}"            }

            is Condition.HasText -> expr.text

            is Condition.Or ->
                expr.operands.joinToString(prefix = if (isOuter) "" else "(", separator = " or ", postfix = if (isOuter) "" else ")") {
                    toString(it)
                }

            is Condition.And ->
                expr.operands.joinToString(separator = " ") {
                    toString(it)
                }
        }
    }

    private fun append(list: MutableList<String>, orders: List<SortOrder>) {
        if (orders.isNotEmpty()) {
            orders.forEach { order ->
                list.add(when (order) {
                    is SortOrder.Book -> dot(order) + "o.b"
                    is SortOrder.Scheduled -> dot(order) + "o.s"
                    is SortOrder.Deadline -> dot(order) + "o.d"
                    is SortOrder.Priority -> dot(order) + "o.p"
                    is SortOrder.State -> dot(order) + "o.state"
                })
            }
        }
    }

    private fun append(list: MutableList<String>, options: Options) {
        val default = Options()

        if (options != default) {
            if (default.agendaDays != options.agendaDays) {
                list.add("ad.${options.agendaDays}")
            }
        }
    }


    private fun quote(s: String) = QuotedStringTokenizer.quote(s, " ")

    private fun dot(order: SortOrder) = if (order.desc) "." else ""
}