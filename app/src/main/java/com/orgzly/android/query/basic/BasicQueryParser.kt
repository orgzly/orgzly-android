package com.orgzly.android.query.basic

import android.provider.ContactsContract
import com.orgzly.android.query.*

open class BasicQueryParser : QueryParser() {
    override val groupOpen   = "("
    override val groupClose  = ")"

    override val operatorsAnd = listOf("and", "&")
    override val operatorsOr = listOf("or", "|")

    override val conditions = listOf(
            ConditionMatch("""^(-)?book:(.*)""", { matcher ->
                Condition.InBook(unQuote(matcher.group(2)), matcher.group(1) != null)
            }),

            ConditionMatch("""^(-)?state:(.*)""", { matcher ->
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

            ConditionMatch("""^(-)?tag:(.*)""", { matcher ->
                Condition.HasTag(unQuote(matcher.group(2)), matcher.group(1) != null)
            }),

            ConditionMatch("""^own-tag:(.*)""", { matcher ->
                Condition.HasOwnTag(unQuote(matcher.group(1)))
            }),

            // scheduled:<3d
            ConditionMatch("""^(scheduled|deadline):(?:(!=|<|<=|>|>=))?(.*)""", { matcher ->
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
                    if (timeTypeMatch == "scheduled") {
                        Condition.Scheduled(interval, relation)
                    } else {
                        Condition.Deadline(interval, relation)
                    }
                } else {
                    null // Ignore this match
                }
            })
    )


    override val sortOrders = listOf(
            SortOrderMatch("""^(-)?sort-order:(scheduled|sched|s)$""", { matcher ->
                SortOrder.ByScheduled(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(deadline|dead|d)$""", { matcher ->
                SortOrder.ByDeadline(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(priority|prio|pri|p)$""", { matcher ->
                SortOrder.ByPriority(matcher.group(1) != null)
            }),
            SortOrderMatch("""^(-)?sort-order:(notebook|book|b)$""", { matcher ->
                SortOrder.ByBook(matcher.group(1) != null)
            })
    )

    override val supportedOptions = listOf(
            OptionMatch("""^agenda-days:(\d+)$""", { matcher, options ->
                val days = matcher.group(1).toInt()
                if (days > 0) options.copy(agendaDays = days) else null
            })
    )
}
