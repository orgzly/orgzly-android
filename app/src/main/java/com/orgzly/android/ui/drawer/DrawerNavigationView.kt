package com.orgzly.android.ui.drawer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import androidx.annotation.ColorInt
import androidx.lifecycle.Observer
import com.google.android.material.navigation.NavigationView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookAction
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.books.BooksFragment
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.main.MainActivityViewModel
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.notes.query.QueryFragment
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import java.util.*


internal class DrawerNavigationView(
        private val activity: MainActivity,
        viewModel: MainActivityViewModel,
        navView: NavigationView) {

    private val menu: Menu = navView.menu

    private val menuItemIdMap = hashMapOf<String, Int>()

    private var activeFragmentTag: String? = null

    init {
        // Add mapping for groups
        menuItemIdMap[BooksFragment.drawerItemId] = R.id.books
        menuItemIdMap[SavedSearchesFragment.getDrawerItemId()] = R.id.searches

        // Setup intents
        menu.findItem(R.id.searches).intent = Intent(AppIntent.ACTION_OPEN_SAVED_SEARCHES)
        menu.findItem(R.id.books).intent = Intent(AppIntent.ACTION_OPEN_BOOKS)
        menu.findItem(R.id.settings).intent = Intent(AppIntent.ACTION_OPEN_SETTINGS)

        viewModel.books().observe(activity, Observer {
            refreshFromBooks(it)
        })

        viewModel.savedSearches().observe(activity, Observer {
            refreshFromSavedSearches(it)
        })
    }

    fun updateActiveFragment(fragmentTag: String) {
        this.activeFragmentTag = fragmentTag

        setActiveItem(fragmentTag)
    }

    private fun setActiveItem(fragmentTag: String) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, fragmentTag)

        this.activeFragmentTag = fragmentTag

        val fragment = activity.supportFragmentManager.findFragmentByTag(activeFragmentTag)

        // Uncheck all
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = false
        }

        if (fragment != null && fragment is DrawerItem) {
            val fragmentMenuItemId = fragment.getCurrentDrawerItemId()

            val itemId = menuItemIdMap[fragmentMenuItemId]

            if (itemId != null) {
                menu.findItem(itemId)?.isChecked = true
            }
        }
    }

    private fun refreshFromSavedSearches(savedSearches: List<SavedSearch>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedSearches.size)

        removeItemsWithOrder(1)

        savedSearches.forEach { savedSearch ->
            val intent = Intent(AppIntent.ACTION_OPEN_QUERY)
            intent.putExtra(AppIntent.EXTRA_QUERY_STRING, savedSearch.query)

            val id = generateRandomUniqueId()
            val item = menu.add(R.id.drawer_group, id, 1, savedSearch.name)

            menuItemIdMap[QueryFragment.getDrawerItemId(savedSearch.query)] = id

            item.intent = intent
            item.isCheckable = true
        }

        activeFragmentTag?.let {
            setActiveItem(it)
        }
    }

    private fun refreshFromBooks(books: List<BookView>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, books.size)

        removeItemsWithOrder(3)

        val attrs = getAttributes()

        books.forEach { book ->
            val intent = Intent(AppIntent.ACTION_OPEN_BOOK)
            intent.putExtra(AppIntent.EXTRA_BOOK_ID, book.book.id)

            val id = generateRandomUniqueId()
            val item = menu.add(R.id.drawer_group, id, 3, getBookText(book.book, attrs))

            item.intent = intent
            item.isCheckable = true

            if (book.book.lastAction?.type == BookAction.Type.ERROR) {
                item.setActionView(R.layout.drawer_item_sync_failed)

            } else if (book.isOutOfSync()) {
                item.setActionView(R.layout.drawer_item_sync_needed)
            }

            menuItemIdMap[BookFragment.getDrawerItemId(book.book.id)] = id
        }

        activeFragmentTag?.let {
            setActiveItem(it)
        }
    }

    private data class Attributes(@ColorInt val mutedTextColor: Int)

    private fun getBookText(book: Book, attr: Attributes): CharSequence {
        val sb = SpannableString(book.title ?: book.name)

        if (book.isDummy) {
            sb.setSpan(ForegroundColorSpan(attr.mutedTextColor), 0, sb.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }

        return sb
    }

    @SuppressLint("ResourceType")
    private fun getAttributes(): Attributes {
        return activity.styledAttributes(intArrayOf(R.attr.text_disabled_color)) { typedArray ->
            Attributes(typedArray.getColor(0, 0))
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

    companion object {
        private val TAG = DrawerNavigationView::class.java.name
    }
}
