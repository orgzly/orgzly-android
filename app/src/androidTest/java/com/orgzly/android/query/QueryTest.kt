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
class QueryTest(private val param: Parameter) : OrgzlyTest() {

    private lateinit var actualQueryString: String
    private lateinit var actualSqlSelection: String
    private lateinit var actualSqlSelectionArgs: List<String>
    private lateinit var actualSqlOrder: String
    private lateinit var actualQuerySortOrders: List<SortOrder>
    private lateinit var actualQueryOptions: Options

    data class Parameter(
            val queryString: String,
            val expectedQueryString: String? = null,
            val expectedSqlSelection: String? = null,
            val expectedSelectionArgs: List<String>? = null,
            val expectedSqlOrder: String? = null,
            val expectedQuerySortOrders: List<SortOrder>? = null,
            val expectedQueryOptions: Options? = null
    )

    companion object {
        @JvmStatic @Parameterized.Parameters
        fun data(): Collection<Parameter> {
            return listOf(
                    Parameter(
                            queryString = "i.todo",
                            expectedQueryString = "i.todo",
                            expectedSqlSelection = "COALESCE(state, '') = ?",
                            expectedSelectionArgs = listOf("TODO")
                    ),
                    Parameter(
                            queryString = "i.todo t.work",
                            expectedQueryString = "i.todo t.work",
                            expectedSqlSelection = "COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            expectedSelectionArgs = listOf("TODO", "%work%", "%work%")
                    ),
                    Parameter(
                            queryString = "i.todo or t.work",
                            expectedQueryString = "i.todo or t.work",
                            expectedSqlSelection = "COALESCE(state, '') = ? OR (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            expectedSelectionArgs = listOf("TODO", "%work%", "%work%"),
                            expectedQuerySortOrders = listOf()
                    ),
                    Parameter(
                            queryString = "i.todo or i.next",
                            expectedQueryString = "i.todo or i.next",
                            expectedSqlSelection = "COALESCE(state, '') = ? OR COALESCE(state, '') = ?",
                            expectedSelectionArgs = listOf("TODO", "NEXT")
                    ),
                    Parameter(
                            queryString = "i.todo or i.next and t.work",
                            expectedQueryString = "i.todo or i.next t.work",
                            expectedSqlSelection = "COALESCE(state, '') = ? OR (COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            expectedSelectionArgs = listOf("TODO", "NEXT", "%work%", "%work%")
                    ),
                    Parameter(
                            queryString = "i.todo and t.work or i.next",
                            expectedQueryString = "i.todo t.work or i.next",
                            expectedSqlSelection = "(COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)) OR COALESCE(state, '') = ?",
                            expectedSelectionArgs = listOf("TODO", "%work%", "%work%", "NEXT")
                    ),
                    Parameter(
                            queryString = "i.todo t.work or i.next t.home",
                            expectedQueryString = "i.todo t.work or i.next t.home",
                            expectedSqlSelection = "(COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)) OR (COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            expectedSelectionArgs = listOf("TODO", "%work%", "%work%", "NEXT", "%home%", "%home%")
                    ),
                    Parameter(
                            queryString = "( i.todo t.work ) or i.next",
                            expectedQueryString = "i.todo t.work or i.next",
                            expectedSqlSelection = "(COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)) OR COALESCE(state, '') = ?",
                            expectedSelectionArgs = listOf("TODO", "%work%", "%work%", "NEXT")
                    ),
                    Parameter(
                            queryString = "i.todo (i.next or t.work)",
                            expectedQueryString = "i.todo (i.next or t.work)",
                            expectedSqlSelection = "COALESCE(state, '') = ? AND (COALESCE(state, '') = ? OR (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            expectedSelectionArgs = listOf("TODO", "NEXT", "%work%", "%work%")
                    ),
                    Parameter(
                            queryString = "(( i.todo) )",
                            expectedQueryString = "i.todo",
                            expectedSqlSelection = "(COALESCE(state, '') = ?)",
                            expectedSelectionArgs = listOf("TODO")
                    ),
                    Parameter(
                            queryString = "(it.todo b.gtd )or .s.none",
                            expectedQueryString = "it.todo b.gtd or .s.none",
                            expectedSqlSelection = "(COALESCE(state, '') IN (?, ?) AND book_name = ?) OR (title LIKE ? OR content LIKE ? OR tags LIKE ?)",
                            expectedSelectionArgs = listOf("TODO", "NEXT", "gtd", "%.s.none%", "%.s.none%", "%.s.none%")
                    ),
                    Parameter(
                            queryString = "it.todo",
                            expectedQueryString = "it.todo",
                            expectedSqlSelection = "COALESCE(state, '') IN (?, ?)",
                            expectedSelectionArgs = listOf("TODO", "NEXT")
                    ),
                    Parameter(
                            queryString = ".it.none",
                            expectedQueryString = ".it.none",
                            expectedSqlSelection = "NOT(COALESCE(state, '') = '')",
                            expectedSelectionArgs = listOf()
                    ),
                    Parameter(
                            queryString = "i.todo (t.work or o.p i.next) .o.book t.home",
                            expectedQueryString = "i.todo (t.work or i.next) t.home o.p .o.b",
                            expectedSqlSelection = "COALESCE(state, '') = ? AND ((COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?) OR COALESCE(state, '') = ?) AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            expectedSelectionArgs = listOf("TODO", "%work%", "%work%", "NEXT", "%home%", "%home%"),
                            expectedQuerySortOrders = listOf(SortOrder.Priority(), SortOrder.Book(desc = true))
                    ),
                    Parameter(
                            queryString = ".i.done ( t.t1 or t.t2)",
                            expectedQueryString = ".i.done (t.t1 or t.t2)",
                            expectedSqlSelection = "NOT(COALESCE(state, '') = ?) AND ((COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?) OR (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            expectedSelectionArgs = listOf("DONE", "%t1%", "%t1%", "%t2%", "%t2%")
                    ),
                    Parameter(
                            queryString = "tnn.tag1",
                            expectedQueryString = "tnn.tag1",
                            expectedSqlSelection = "(title LIKE ? OR content LIKE ? OR tags LIKE ?)",
                            expectedSelectionArgs = listOf("%tnn.tag1%", "%tnn.tag1%", "%tnn.tag1%")
                    ),
                    Parameter(
                            queryString = "p.",
                            expectedQueryString = "p.",
                            expectedSqlSelection = "(title LIKE ? OR content LIKE ? OR tags LIKE ?)",
                            expectedSelectionArgs = listOf("%p.%", "%p.%", "%p.%")
                    ),
                    Parameter( // Operator with no expression before it
                            queryString = "and t.tag",
                            expectedQueryString = "t.tag",
                            expectedSqlSelection = "(COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            expectedSelectionArgs = listOf("%tag%", "%tag%")
                    ),
                    Parameter(
                            queryString = "i.todo (b.\"book(1) name\" or b.book2)",
                            expectedQueryString = "i.todo (b.\"book(1) name\" or b.book2)",
                            expectedSqlSelection = "COALESCE(state, '') = ? AND (book_name = ? OR book_name = ?)",
                            expectedSelectionArgs = listOf("TODO", "book(1) name", "book2")
                    ),
                    Parameter(
                            queryString = "s.le.3d",
                            expectedQueryString = "s.3d",
                            expectedSqlSelection = "(scheduled_time_timestamp != 0 AND scheduled_time_timestamp < " + TimeUtils.timeFromNow(Calendar.DAY_OF_MONTH, 3+1) + ")"
                    ),
                    Parameter(
                            queryString = "d.tom",
                            expectedQueryString = "d.tomorrow",
                            expectedSqlSelection = "(deadline_time_timestamp != 0 AND deadline_time_timestamp < " + TimeUtils.timeFromNow(Calendar.DAY_OF_MONTH, 1+1) + ")"
                    ),
                    Parameter(
                            queryString = "c.eq.today",
                            expectedQueryString = "c.today",
                            expectedSqlSelection = "(closed_time_timestamp != 0 AND ${TimeUtils.timeFromNow(Calendar.DAY_OF_MONTH, 0)} <= closed_time_timestamp AND closed_time_timestamp < " + TimeUtils.timeFromNow(Calendar.DAY_OF_MONTH, 0+1) + ")"
                    ),
                    Parameter(
                            queryString = "c.ge.-1d", // Since yesterday
                            expectedQueryString = "c.ge.yesterday",
                            expectedSqlSelection = "(closed_time_timestamp != 0 AND ${TimeUtils.timeFromNow(Calendar.DAY_OF_MONTH, -1)} <= closed_time_timestamp)"
                    ),
                    Parameter(
                            queryString = "p.a",
                            expectedQueryString = "p.a",
                            expectedSqlSelection = "LOWER(COALESCE(NULLIF(priority, ''), ?)) = ?",
                            expectedSelectionArgs = listOf("B", "a") // TODO: Normalize
                    ),
                    Parameter(
                            queryString = "ad.2",
                            expectedQueryString = "ad.2",
                            expectedSqlSelection = "",
                            expectedSelectionArgs = listOf()
                    ),
                    Parameter(
                            queryString = "o.state",
                            expectedQueryString = "o.state",
                            expectedSqlSelection = "",
                            expectedSelectionArgs = listOf(),
                            expectedSqlOrder = "CASE state WHEN 'TODO' THEN 0 WHEN 'NEXT' THEN 1 WHEN 'DONE' THEN 2 ELSE 3 END, is_visible",
                            expectedQuerySortOrders = listOf(SortOrder.State()),
                            expectedQueryOptions = Options()
                    ),
                    Parameter(
                            queryString = "s.ge.3d",
                            expectedQueryString = "s.ge.3d",
                            expectedSqlSelection = "(${DbNoteView.SCHEDULED_TIME_TIMESTAMP} != 0 AND ${TimeUtils.timeFromNow(Calendar.DAY_OF_MONTH, 3)} <= ${DbNoteView.SCHEDULED_TIME_TIMESTAMP})"
                    )
            )
        }
    }

    @Before
    override fun setUp() {
        super.setUp()

        // Parse query
        val parser = DottedQueryParser()
        val query = parser.parse(param.queryString)

        // Build SQL
        val sqlBuilder: SqlQueryBuilder = SqliteQueryBuilder(context)
        sqlBuilder.build(query)

        // Build query
        val queryBuilder = DottedQueryBuilder(context)
        queryBuilder.build(query)

        actualQuerySortOrders = query.sortOrders
        actualQueryOptions = query.options

        actualSqlSelection = sqlBuilder.getSelection()
        actualSqlSelectionArgs = sqlBuilder.getSelectionArgs()
        actualSqlOrder = sqlBuilder.getOrderBy()

        actualQueryString = queryBuilder.getString()
    }

    @Test
    fun testSortOrders() {
        param.expectedQuerySortOrders?.let {
            assertThat(param.queryString, actualQuerySortOrders, `is`(param.expectedQuerySortOrders))
        }
    }

    @Test
    fun testOptions() {
        param.expectedQueryOptions?.let {
            assertThat(param.queryString, actualQueryOptions, `is`(param.expectedQueryOptions))
        }
    }

    @Test
    fun testSqlQuery() {
        param.expectedSqlSelection?.let {
            assertThat(param.queryString, actualSqlSelection, `is`(param.expectedSqlSelection))
        }
    }

    @Test
    fun testSqlArgs() {
        param.expectedSelectionArgs?.let {
            assertThat(param.queryString, actualSqlSelectionArgs, `is`(param.expectedSelectionArgs))
        }
    }

    @Test
    fun testSqlOrder() {
        param.expectedSqlOrder?.let {
            assertThat(param.queryString, actualSqlOrder, `is`(param.expectedSqlOrder))
        }
    }

    @Test
    fun testBuiltQuery() {
        param.expectedQueryString?.let {
            assertThat(param.queryString, actualQueryString, `is`(param.expectedQueryString))
        }
    }
}