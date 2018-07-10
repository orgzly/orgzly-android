package com.orgzly.android.ui.drawer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.database.Cursor
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.design.widget.NavigationView
import android.support.v4.content.Loader
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.Book
import com.orgzly.android.BookUtils
import com.orgzly.android.provider.GenericDatabaseUtils
import com.orgzly.android.provider.ProviderContract
import com.orgzly.android.provider.clients.BooksClient
import com.orgzly.android.provider.clients.FiltersClient
import com.orgzly.android.ui.Loaders
import com.orgzly.android.ui.MainActivity
import com.orgzly.android.ui.fragments.BookFragment
import com.orgzly.android.ui.fragments.BooksFragment
import com.orgzly.android.ui.fragments.FiltersFragment
import com.orgzly.android.ui.fragments.QueryFragment
import com.orgzly.android.util.LogUtils
import java.util.*


internal class DrawerNavigationView(private val activity: MainActivity, navView: NavigationView) :
        android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

    private val menu: Menu = navView.menu

    private val menuItemIdMap = hashMapOf<String, Int>()

    private var activeFragmentTag: String? = null

    init {
        // Add mapping for groups
        menuItemIdMap[BooksFragment.getDrawerItemId()] = R.id.books
        menuItemIdMap[FiltersFragment.getDrawerItemId()] = R.id.searches

        // Setup intents
        menu.findItem(R.id.searches).intent = Intent(AppIntent.ACTION_OPEN_QUERIES)
        menu.findItem(R.id.books).intent = Intent(AppIntent.ACTION_OPEN_BOOKS)
        menu.findItem(R.id.settings).intent = Intent(AppIntent.ACTION_OPEN_SETTINGS)

        activity.supportLoaderManager.initLoader(Loaders.DRAWER_BOOKS, null, this)
        activity.supportLoaderManager.initLoader(Loaders.DRAWER_FILTERS, null, this)
    }

    fun updateActiveFragment(fragmentTag: String) {
        this.activeFragmentTag = fragmentTag

        setActiveItem(fragmentTag)
    }

    private fun setActiveItem(fragmentTag: String) {
        this.activeFragmentTag = fragmentTag

        val fragment = activity.supportFragmentManager.findFragmentByTag(activeFragmentTag)

        if (fragment != null && fragment is DrawerItem) {
            val fragmentMenuItemId = fragment.getCurrentDrawerItemId()

            val itemId = menuItemIdMap[fragmentMenuItemId]

            if (itemId != null) {
                menu.findItem(itemId)?.isChecked = true
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return when (id) {
            Loaders.DRAWER_FILTERS -> FiltersClient.getCursorLoader(activity)
            Loaders.DRAWER_BOOKS -> BooksClient.getCursorLoader(activity)
            else -> throw IllegalStateException("Loader $id is unknown")
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader)

        when (loader.id) {
            Loaders.DRAWER_FILTERS -> updateQueriesFromCursor(cursor)
            Loaders.DRAWER_BOOKS -> updateBooksFromCursor(cursor)
        }

        activeFragmentTag?.let {
            setActiveItem(it)
        }
    }

    private fun updateQueriesFromCursor(cursor: Cursor) {
        removeItemsWithOrder(1)

        GenericDatabaseUtils.forEachRow(cursor) {
            val name = cursor.getString(cursor.getColumnIndex(ProviderContract.Filters.Param.NAME))
            val query = cursor.getString(cursor.getColumnIndex(ProviderContract.Filters.Param.QUERY))

            val intent = Intent(AppIntent.ACTION_OPEN_QUERY)
            intent.putExtra(AppIntent.EXTRA_QUERY_STRING, query)

            val id = generateRandomUniqueId()
            val item = menu.add(R.id.drawer_group, id, 1, name)

            menuItemIdMap[QueryFragment.getDrawerItemId(query)] = id

            item.intent = intent
            item.isCheckable = true

        }
    }

    private fun updateBooksFromCursor(cursor: Cursor) {
        removeItemsWithOrder(3)

        val attrs = getAttributes()

        GenericDatabaseUtils.forEachRow(cursor) {
            val book = BooksClient.fromCursor(cursor)

            val intent = Intent(AppIntent.ACTION_OPEN_BOOK)
            intent.putExtra(AppIntent.EXTRA_BOOK_ID, book.id)

            val id = generateRandomUniqueId()
            val item = menu.add(R.id.drawer_group, id, 3, getBookText(book, attrs))

            item.intent = intent
            item.isCheckable = true

            if (book.isModifiedAfterLastSync) {
                item.setActionView(R.layout.drawer_item_action)
            }

            menuItemIdMap[BookFragment.getDrawerItemId(book.id)] = id
        }
    }

    private data class Attributes(@ColorInt val mutedTextColor: Int)

    private fun getBookText(book: Book, attr: Attributes): CharSequence {
        val name = BookUtils.getFragmentTitleForBook(book)

        val sb = SpannableString(name)

        if (book.isDummy) {
            sb.setSpan(ForegroundColorSpan(attr.mutedTextColor), 0, sb.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }

        return sb
    }

    @SuppressLint("ResourceType")
    private fun getAttributes(): Attributes {
        val typedArray = activity.obtainStyledAttributes(intArrayOf(R.attr.text_disabled_color))
        try {
            return Attributes(typedArray.getColor(0, 0))
        } finally {
            typedArray.recycle()
        }
    }

    private fun generateRandomUniqueId(): Int {
        val rand = Random()

        while (true) {
            val id = rand.nextInt(Integer.MAX_VALUE) + 1

            try {
                activity.resources.getResourceName(id)
            } catch (e: Resources.NotFoundException) {
                return id
            }
        }
    }

    private fun removeItemsWithOrder(orderToDelete: Int) {
        val itemIdsToRemove = HashSet<Int>()

        var i = 0
        while (true) {
            val item = menu.getItem(i++) ?: break

            val order = item.order

            if (order > orderToDelete) {
                break

            } else if (order == orderToDelete) {
                itemIdsToRemove.add(item.itemId)
            }
        }

        for (id in itemIdsToRemove) {
            menu.removeItem(id)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }

    companion object {
        private val TAG = DrawerNavigationView::class.java.name
    }
}
