package com.orgzly.android.query.basic

import com.orgzly.android.query.*

open class BasicQueryParser : QueryParser() {
    override val groupOpen   = "("
    override val groupClose  = ")"

    override val operatorsAnd = listOf("and", "&")
    override val operatorsOr = listOf("or", "|")

    override val conditions = listOf(
            ConditionMatch("""^(-)?book:(.+)""", { matcher ->
                Condition.InBook(unQuote(matcher.group(2)), matcher.group(1) != null)
            }),

            ConditionMatch("""^(-)?state:(.+)""", { matcher ->
                Condition.HasState(unQuote(matcher.group(2)), matcher.group(1) != null)
            }),

            ConditionMatch("""^(-)?state-type:(todo|done|none)""", { matcher ->
                val stateType = StateType.valueOf(matcher.group(2).toUpperCase())
                Condition.HasStateType(stateType, matcher.group(1) != null)
            }),

            ConditionMatch("""^(-)?priority:([a-zA-Z])""", { matcher ->
                Condition.HasPriority(matcher.group(2), matcher.group(1) != null)
            }),

            ConditionMatch("""^(-)?set-priority:([a-zA-Z])""", { matcher ->
                Condition.HasSetPriority(matcher.group(2), matcher.group(1) != null)
            }),

            ConditionMatch("""^(-)?tag:(.+)""", { matcher ->
                Condition.HasTag(unQuote(matcher.group(2)), matcher.group(1) != null)
            }),

            ConditionMatch("""^own-tag:(.+)""", { matcher ->
                Condition.HasOwnTag(unQuote(matcher.group(1)))
            }),

            // scheduled:<3d
            ConditionMatch("""^(scheduled|deadline|closed):(?:(!=|<|<=|>|>=))?(.+)""", { matcher ->
                val timeTypeMatch = matcher.group(1)
                val relationMatch = matcher.group(2)
                val intervalMatch = matcher.group(3)

                val relation = when (relationMatch) {
                    "!=" -> Relation.NE
                    "<"  -> Relation.LT
                    "<=" -> Relation.LE
                    ">"  -> Relation.GT
                    ">=" -> Relation.GE
                    else -> Relation.EQ // Default if there is no relation
                }

                val interval = QueryInterval.parse(unQuote(intervalMatch))

                if (interval != null) {
                    when (timeTypeMatch) {
                        "deadline" -> Condition.Deadline(interval, relation)
                        "closed"   -> Condition.Closed(interval, relation)
                        else       -> Condition.Scheduled(interval, relation)
                    }
                } else {
                    null // Ignore this match
                }
            })
    )


    override val sortOrders = listOf(
            SortOrderMatch("""^(-)?sort-order:(?:scheduled|sched)$""", { matcher ->
                SortOrder.Scheduled(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(?:deadline|dead)$""", { matcher ->
                SortOrder.Deadline(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(?:closed|close)$""", { matcher ->
                SortOrder.Closed(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(?:priority|prio)$""", { matcher ->
                SortOrder.Priority(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(?:notebook|book)$""", { matcher ->
                SortOrder.Book(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(?:state|st)$""", { matcher ->
                SortOrder.State(matcher.group(1) != null)
            })

    )

    override val supportedOptions = listOf(
            OptionMatch("""^agenda-days:(\d+)$""", { matcher, options ->
                val days = matcher.group(1).toInt()
                if (days > 0) options.copy(agendaDays = days) else null
            })
    )
}
