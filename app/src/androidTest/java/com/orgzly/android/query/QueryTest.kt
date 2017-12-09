package com.orgzly.android.query

import android.support.test.espresso.matcher.ViewMatchers.assertThat
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.prefs.AppPreferences
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
class QueryTest(
        private val queryString: String,
        private val expectedQueryString: String,
        private val expectedSql: String,
        private val expectedArgs: List<String>,
        private val expectedSortOrders: List<SortOrder>
        ) : OrgzlyTest() {

    private lateinit var sortOrders: List<SortOrder>
    private lateinit var sqlSelection: String
    private lateinit var sqlSelectionArgs: List<String>
    private lateinit var builtQueryString: String

    companion object {
        @JvmStatic @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                    arrayOf(
                            "i.todo",
                            "i.todo",
                            "COALESCE(state, '') = ?",
                            listOf("TODO"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo t.work",
                            "i.todo t.work",
                            "COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            listOf("TODO", "%work%", "%work%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo or t.work",
                            "i.todo or t.work",
                            "COALESCE(state, '') = ? OR (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            listOf("TODO", "%work%", "%work%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo or i.next",
                            "i.todo or i.next",
                            "COALESCE(state, '') = ? OR COALESCE(state, '') = ?",
                            listOf("TODO", "NEXT"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo or i.next and t.work",
                            "i.todo or i.next t.work",
                            "COALESCE(state, '') = ? OR (COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            listOf("TODO", "NEXT", "%work%", "%work%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo and t.work or i.next",
                            "i.todo t.work or i.next",
                            "(COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)) OR COALESCE(state, '') = ?",
                            listOf("TODO", "%work%", "%work%", "NEXT"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo t.work or i.next t.home",
                            "i.todo t.work or i.next t.home",
                            "(COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)) OR (COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            listOf("TODO", "%work%", "%work%", "NEXT", "%home%", "%home%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "( i.todo t.work ) or i.next",
                            "i.todo t.work or i.next",
                            "(COALESCE(state, '') = ? AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)) OR COALESCE(state, '') = ?",
                            listOf("TODO", "%work%", "%work%", "NEXT"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo (i.next or t.work)",
                            "i.todo (i.next or t.work)",
                            "COALESCE(state, '') = ? AND (COALESCE(state, '') = ? OR (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            listOf("TODO", "NEXT", "%work%", "%work%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "(( i.todo) )",
                            "i.todo",
                            "(COALESCE(state, '') = ?)",
                            listOf("TODO"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "(it.todo b.gtd )or .s.none",
                            "it.todo b.gtd or .s.none",
                            "(COALESCE(state, '') IN (?, ?) AND book_name = ?) OR (title LIKE ? OR content LIKE ? OR tags LIKE ?)",
                            listOf("TODO", "NEXT", "gtd", "%.s.none%", "%.s.none%", "%.s.none%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "it.todo",
                            "it.todo",
                            "COALESCE(state, '') IN (?, ?)",
                            listOf("TODO", "NEXT"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            ".it.none",
                            ".it.none",
                            "NOT(COALESCE(state, '') = '')",
                            listOf<String>(),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo (t.work or o.p i.next) .o.book t.home",
                            "i.todo (t.work or i.next) t.home o.p .o.b",
                            "COALESCE(state, '') = ? AND ((COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?) OR COALESCE(state, '') = ?) AND (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            listOf("TODO", "%work%", "%work%", "NEXT", "%home%", "%home%"),
                            listOf(SortOrder.ByPriority(), SortOrder.ByBook(desc = true))
                    ),
                    arrayOf(
                            ".i.done ( t.t1 or t.t2)",
                            ".i.done (t.t1 or t.t2)",
                            "NOT(COALESCE(state, '') = ?) AND ((COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?) OR (COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?))",
                            listOf("DONE", "%t1%", "%t1%", "%t2%", "%t2%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "tnn.tag1",
                            "tnn.tag1",
                            "(title LIKE ? OR content LIKE ? OR tags LIKE ?)",
                            listOf("%tnn.tag1%", "%tnn.tag1%", "%tnn.tag1%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "p.",
                            "p.",
                            "(title LIKE ? OR content LIKE ? OR tags LIKE ?)",
                            listOf("%p.%", "%p.%", "%p.%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf( // Operator with no expression before it
                            "and t.tag",
                            "t.tag",
                            "(COALESCE(tags, '') LIKE ? OR COALESCE(inherited_tags, '') LIKE ?)",
                            listOf("%tag%", "%tag%"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "i.todo (b.\"book(1) name\" or b.book2)",
                            "i.todo (b.\"book(1) name\" or b.book2)",
                            "COALESCE(state, '') = ? AND (book_name = ? OR book_name = ?)",
                            listOf("TODO", "book(1) name", "book2"),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "s.3d",
                            "s.3d",
                            "(scheduled_time_timestamp != 0 AND scheduled_time_timestamp < " + TimeUtils.dayAfter(Calendar.DAY_OF_MONTH, 3).timeInMillis + ")",
                            listOf<String>(),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "d.tom",
                            "d.tomorrow",
                            "(deadline_time_timestamp != 0 AND deadline_time_timestamp < " + TimeUtils.dayAfter(Calendar.DAY_OF_MONTH, 1).timeInMillis + ")",
                            listOf<String>(),
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "p.a",
                            "p.a",
                            "LOWER(COALESCE(NULLIF(priority, ''), ?)) = ?",
                            listOf("B", "a"), // TODO: Normalize
                            listOf<SortOrder>()
                    ),
                    arrayOf(
                            "s.-123d",
                            "",
                            "",
                            listOf<String>(),
                            listOf<SortOrder>()
                    )
            )
        }
    }

    @Before
    override fun setUp() {
        super.setUp()

        // Parse query
        val parser = DottedQueryParser()
        val query = parser.parse(queryString)

        // Generate SQL
        val sqlBuilder: SqlQueryBuilder = SqliteQueryBuilder(context)
        sqlBuilder.build(query)

        sortOrders = query.sortOrders

        sqlSelection = sqlBuilder.getSelection()
        sqlSelectionArgs = sqlBuilder.getSelectionArgs()

        val queryBuilder = DottedQueryBuilder(context)
        queryBuilder.build(query)
        builtQueryString = queryBuilder.getString()

    }

    @Test
    fun testSqlQuery() {
        assertThat(queryString, sqlSelection, `is`(expectedSql))
    }

    @Test
    fun testSqlArgs() {
        assertThat(queryString, sqlSelectionArgs, `is`(expectedArgs))
    }

    @Test
    fun testBuiltQuery() {
        assertThat(queryString, builtQueryString, `is`(expectedQueryString))
    }

    @Test
    fun testSortOrders() {
        assertThat(queryString, sortOrders, `is`(expectedSortOrders))
    }
}