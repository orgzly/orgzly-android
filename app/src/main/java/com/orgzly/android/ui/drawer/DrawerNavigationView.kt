package com.orgzly.android.ui.drawer

import android.content.Intent
import android.content.res.Resources
import android.database.Cursor
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.view.Menu
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.BookUtils
import com.orgzly.android.provider.ProviderContract
import com.orgzly.android.provider.clients.BooksClient
import com.orgzly.android.provider.clients.FiltersClient
import com.orgzly.android.ui.Loaders
import com.orgzly.android.ui.MainActivity
import com.orgzly.android.ui.fragments.*
import com.orgzly.android.util.LogUtils
import java.util.*


internal class DrawerNavigationView(private val activity: MainActivity, navView: NavigationView) : android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
    private val menu: Menu = navView.menu

    private val menuItemIdMap = hashMapOf<String, Int>()

    private var activeFragmentTag: String? = null

    init {
        // Add mapping for groups
        menuItemIdMap[BooksFragment.getDrawerItemId()] = R.id.books
        menuItemIdMap[FiltersFragment.getDrawerItemId()] = R.id.queries

        // Setup intents
        menu.findItem(R.id.queries).intent = Intent(AppIntent.ACTION_OPEN_QUERIES)
        menu.findItem(R.id.books).intent = Intent(AppIntent.ACTION_OPEN_BOOKS)
        menu.findItem(R.id.settings).intent = Intent(AppIntent.ACTION_OPEN_SETTINGS)

        // Init loaders
        activity.supportLoaderManager.initLoader(Loaders.DRAWER_BOOKS, null, this)
        activity.supportLoaderManager.initLoader(Loaders.DRAWER_FILTERS, null, this)
    }

    fun refresh(fragmentTag: String) {
        this.activeFragmentTag = fragmentTag

        // Restart Loaders in case settings changed (e.g. notebooks order)
        activity.supportLoaderManager.restartLoader(Loaders.DRAWER_BOOKS, null, this)
        activity.supportLoaderManager.restartLoader(Loaders.DRAWER_FILTERS, null, this)
    }

    private fun setActiveItem(fragmentTag: String) {
        this.activeFragmentTag = fragmentTag

        val fragment = activity.supportFragmentManager.findFragmentByTag(activeFragmentTag)

        if (fragment is DrawerItem) {
            val fragmentMenuItemId = fragment.getCurrentDrawerItemId()

            val itemId = menuItemIdMap[fragmentMenuItemId]

            if (itemId != null) {
                menu.findItem(itemId)?.isChecked = true
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): android.support.v4.content.Loader<Cursor> {
        return when (id) {
            Loaders.DRAWER_FILTERS -> FiltersClient.getCursorLoader(activity)
            Loaders.DRAWER_BOOKS -> BooksClient.getCursorLoader(activity)
            else -> throw IllegalStateException("Loader $id is unknown")
        }
    }

    override fun onLoadFinished(loader: android.support.v4.content.Loader<Cursor>, cursor: Cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(MainActivity.TAG)

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

        forEachRow(cursor) {
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

        forEachRow(cursor) {
            val book = BooksClient.fromCursor(cursor)

            val intent = Intent(AppIntent.ACTION_OPEN_BOOK)
            intent.putExtra(AppIntent.EXTRA_BOOK_ID, book.id)

            val id = generateRandomUniqueId()
            val item = menu.add(R.id.drawer_group, id, 3, BookUtils.getFragmentTitleForBook(book))

            item.intent = intent
            item.isCheckable = true

            if (book.isModifiedAfterLastSync) {
                val icon = getBookOutOfSyncIcon()
                if (icon != 0) {
                    item.setIcon(icon)
                }
            }

            menuItemIdMap[BookFragment.getDrawerItemId(book.id)] = id
        }
    }

    // TODO: Move this to some utility class
    private fun forEachRow(cursor: Cursor, f: (Cursor) -> Unit) {
        cursor.moveToFirst()

        while (!cursor.isAfterLast) {
            f(cursor)

            cursor.moveToNext()
        }
    }

    private fun getBookOutOfSyncIcon(): Int {
        val typedArray = activity.obtainStyledAttributes(R.styleable.Icons)

        val resId = typedArray.getResourceId(R.styleable.Icons_ic_sync_18dp, 0)

        typedArray.recycle()

        return resId
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

    override fun onLoaderReset(loader: android.support.v4.content.Loader<Cursor>) {
    }
}
