package com.orgzly.android.query

data class Query @JvmOverloads constructor(
        val condition: Condition?,
        val sortOrders: List<SortOrder> = listOf(),
        val options: Options = Options()) {

    fun isAgenda(): Boolean = options.agendaDays > 0
}
