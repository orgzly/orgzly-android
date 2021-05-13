package com.orgzly.android.query.user

import com.orgzly.android.query.*

open class DottedQueryBuilder {
    fun build(query: Query): String {
        val list = mutableListOf<String>()

        query.condition?.let {
            append(list, it)
        }
        append(list, query.sortOrders)
        append(list, query.options)

        return list.joinToString(" ")
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
            is Condition.HasOwnTag -> "${dot(expr.not)}tn.${expr.tag}"

            is Condition.Event -> {
                val rel = expr.relation.toString().toLowerCase()
                val relString = if (rel == "eq") "" else ".$rel"
                "e$relString.${expr.interval}"
            }

            is Condition.Scheduled -> {
                val rel = expr.relation.toString().toLowerCase()
                val relString = if (rel == "le") "" else ".$rel"
                "s$relString.${expr.interval}"
            }

            is Condition.Deadline -> {
                val rel = expr.relation.toString().toLowerCase()
                val relString = if (rel == "le") "" else ".$rel"
                "d$relString.${expr.interval}"
            }

            is Condition.Closed -> {
                val rel = expr.relation.toString().toLowerCase()
                val relString = if (rel == "eq") "" else ".$rel"
                "c$relString.${expr.interval}"
            }

            is Condition.Created -> {
                val rel = expr.relation.toString().toLowerCase()
                val relString = if (rel == "le") "" else ".$rel"
                "cr$relString.${expr.interval}"
            }

            is Condition.HasText -> if (expr.isQuoted) {
                quote(expr.text, true)
            } else {
                expr.text
            }

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
                    is SortOrder.Title -> dot(order) + "o.t"
                    is SortOrder.Scheduled -> dot(order) + "o.s"
                    is SortOrder.Deadline -> dot(order) + "o.d"
                    is SortOrder.Event -> dot(order) + "o.e"
                    is SortOrder.Closed -> dot(order) + "o.c"
                    is SortOrder.Priority -> dot(order) + "o.p"
                    is SortOrder.State -> dot(order) + "o.state"
                    is SortOrder.Created -> dot(order) + "o.cr"
                    is SortOrder.Position -> dot(order) + "o.pos"
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


    private fun quote(s: String, unconditionally: Boolean = false): String {
        return if (unconditionally) {
            QueryTokenizer.quoteUnconditionally(s)
        } else {
            QueryTokenizer.quote(s, " ")
        }
    }

    private fun dot(order: SortOrder) = if (order.desc) "." else ""
}