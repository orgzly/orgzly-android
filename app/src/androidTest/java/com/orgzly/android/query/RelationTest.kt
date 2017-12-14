package com.orgzly.android.query

import android.support.test.espresso.matcher.ViewMatchers.assertThat
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.provider.views.DbNoteView
import com.orgzly.android.query.dotted.DottedQueryBuilder
import com.orgzly.android.query.dotted.DottedQueryParser
import com.orgzly.android.query.sqlite.SqliteQueryBuilder
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

@RunWith(value = Parameterized::class)
class RelationTest(
        private val queryString: String,
        private val expectedQueryString: String,
        private val expectedSql: String) : OrgzlyTest() {

    private lateinit var builtQueryString: String
    private lateinit var sqlSelection: String

    companion object {
        private val column = DbNoteView.SCHEDULED_TIME_TIMESTAMP

        @JvmStatic @Parameterized.Parameters
        fun data(): Collection<Array<String>> {
            return listOf(
                    arrayOf("s.ge.3d", "s.ge.3d", "($column != 0 AND ${TimeUtils.timeFromNow(Calendar.DAY_OF_MONTH, 3)} <= $column)")
            )
        }
    }

    @Before
    override fun setUp() {
        super.setUp()

        val query = DottedQueryParser().parse(queryString)

        val sqlBuilder: SqlQueryBuilder = SqliteQueryBuilder(context)
        sqlBuilder.build(query)

        val queryBuilder = DottedQueryBuilder(context)
        queryBuilder.build(query)
        builtQueryString = queryBuilder.getString()

        sqlSelection = sqlBuilder.getSelection()
    }

    @Test
    fun testSqlQuery() {
        assertThat(queryString, sqlSelection, `is`(expectedSql))
    }

    @Test
    fun testBuiltQuery() {
        assertThat(queryString, builtQueryString, `is`(expectedQueryString))
    }
}