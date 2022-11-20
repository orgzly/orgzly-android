package com.orgzly.android

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.util.LogUtils
import com.orgzly.org.OrgFileSettings
import javax.inject.Inject

class SharingShortcutsManager {
    @Inject
    lateinit var dataRepository: DataRepository

    private val categoryTextShareTarget = "com.orgzly.android.directshare.category.SPECIFIC_NOTEBOOK"

    init {
        App.appComponent.inject(this)
    }

    fun replaceDynamicShortcuts(context: Context) {
        App.EXECUTORS.diskIO().execute {
            val t1 = System.currentTimeMillis()

            val shortcuts = createShortcuts(context)
                .take(ShortcutManagerCompat.getMaxShortcutCountPerActivity(context))

            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)

            if (BuildConfig.LOG_DEBUG) {
                val t2 = System.currentTimeMillis()
                LogUtils.d(TAG, "Published ${shortcuts.size} shortcuts in ${t2 - t1}ms")
            }
        }
    }

    private fun createShortcuts(context: Context): List<ShortcutInfoCompat> {
        return dataRepository.getBooks().mapNotNull { bookView -> // FIXME: ANR
            val book = bookView.book

            if (hasRequestedDirectShare(book)) {
                val bookId = book.id

                val id = shortcutIdFromBookId(bookId)
                val name = book.name
                val title = BookUtils.getFragmentTitleForBook(book)
                val icon = IconCompat.createWithResource(context, R.mipmap.cic_shortcut_notebook)
                val categories = setOf(categoryTextShareTarget)
                val intent = ShareActivity.createNewNoteIntent(context).apply {
                    putExtra(AppIntent.EXTRA_BOOK_ID, bookId);
                }

                ShortcutInfoCompat.Builder(context, id)
                    .setShortLabel(name)
                    .setLongLabel(title)
                    .setIcon(icon)
                    .setCategories(categories)
                    .setIntent(intent)
                    // .setRank(1)
                    .build()
            } else {
                null
            }
        }
    }

    private fun hasRequestedDirectShare(book: Book): Boolean {
        val directShare = book.preface?.let {
            OrgFileSettings.fromPreface(it)?.getLastKeywordValue(DIRECT_SHARE)
        }

        return !directShare.isNullOrBlank()
    }

    companion object {
        @JvmStatic
        fun bookIdFromShortcutId(shortcutId: String): Long {
            return shortcutId.split("-")[1].toLong()
        }

        private fun shortcutIdFromBookId(bookId: Long): String {
            return "book-$bookId"
        }

        private val TAG = SharingShortcutsManager::class.java.name

        private const val DIRECT_SHARE = "ORGZLY_DIRECT_SHARE"
    }
}