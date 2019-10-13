package com.orgzly.android.query

import com.orgzly.android.query.user.DottedQueryParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class QueryUtilsTest(private val param: Parameter) {

    data class Parameter(val query: String, val bookName: String?)

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: query {0} should return book name {1}")
        fun data(): Collection<Parameter> {
            return listOf(
                    Parameter("b.foo", "foo"),
                    Parameter("b.foo b.bar", "foo"),
                    Parameter("foo or b.bar", "bar"),
                    Parameter("", null)
            )
        }
    }

    @Test
    fun testExtractFirstBookNameFromQuery() {
        val condition = DottedQueryParser().parse(param.query).condition
        val result = QueryUtils.extractFirstBookNameFromQuery(condition)
        assertEquals(param.bookName, result)
    }
}