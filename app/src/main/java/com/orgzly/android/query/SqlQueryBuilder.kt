package com.orgzly.android.query

interface SqlQueryBuilder : QueryBuilder {
    fun getSelection(): String

    fun getSelectionArgs(): List<String>

    fun getOrderBy(): String
}
