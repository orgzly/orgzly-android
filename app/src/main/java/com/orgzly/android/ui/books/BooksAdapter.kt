package com.orgzly.android.ui.books

import android.content.Context
import android.graphics.Typeface
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.R
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookAction
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection


class BooksAdapter(
        private val clickListener: OnViewHolderClickListener<BookView>
) : ListAdapter<BookView, BooksAdapter.ViewHolder>(DIFF_CALLBACK), SelectableItemAdapter {

    private val adapterSelection: Selection = Selection()

    inner class ViewHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view),
            View.OnClickListener,
            View.OnLongClickListener {

        val container: View = view.findViewById(R.id.item_book_container)

        val title: TextView = view.findViewById(R.id.item_book_title)
        val subTitle: TextView = view.findViewById(R.id.item_book_subtitle)

        val syncNeeded: ImageView = view.findViewById(R.id.item_book_sync_needed_icon)
        val syncFailed: ImageView = view.findViewById(R.id.item_book_sync_failed_icon)

        val bookDetailsPadding: View = view.findViewById(R.id.item_book_details_padding)

        val mtimeContainer: View = view.findViewById(R.id.item_book_mtime_container)
        val mtime: TextView = view.findViewById(R.id.item_book_mtime)

        val linkContainer: View = view.findViewById(R.id.item_book_link_container)
        val link: TextView = view.findViewById(R.id.item_book_link_repo)

        val syncedToUrlContainer: View = view.findViewById(R.id.item_book_synced_url_container)
        val syncedToUrl: TextView = view.findViewById(R.id.item_book_synced_url)

        val syncedToMtimeContainer: View = view.findViewById(R.id.item_book_synced_mtime_container)
        val syncedToMtime: TextView = view.findViewById(R.id.item_book_synced_mtime)

        val syncedToRevisionContainer: View = view.findViewById(R.id.item_book_synced_revision_container)
        val syncedToRevision: TextView = view.findViewById(R.id.item_book_synced_revision)

        val lastActionContainer: View = view.findViewById(R.id.item_book_last_action_container)
        val lastAction: TextView = view.findViewById(R.id.item_book_last_action)

        val usedEncodingContainer: View = view.findViewById(R.id.item_book_encoding_used_container)
        val usedEncoding: TextView = view.findViewById(R.id.item_book_encoding_used)

        val detectedEncodingContainer: View = view.findViewById(R.id.item_book_encoding_detected_container)
        val detectedEncoding: TextView = view.findViewById(R.id.item_book_encoding_detected)

        val selectedEncodingContainer: View = view.findViewById(R.id.item_book_encoding_selected_container)
        val selectedEncoding: TextView = view.findViewById(R.id.item_book_encoding_selected)

        val noteCountContainer: View = view.findViewById(R.id.item_book_note_count_container)
        val noteCount: TextView = view.findViewById(R.id.item_book_note_count)

        val containerToPreference = mapOf(
                Pair(mtimeContainer, R.string.pref_value_book_details_mtime),
                Pair(linkContainer, R.string.pref_value_book_details_link_url),
                Pair(syncedToUrlContainer, R.string.pref_value_book_details_sync_url),
                Pair(syncedToMtimeContainer, R.string.pref_value_book_details_sync_mtime),
                Pair(syncedToRevisionContainer, R.string.pref_value_book_details_sync_revision),
                Pair(selectedEncodingContainer, R.string.pref_value_book_details_encoding_selected),
                Pair(detectedEncodingContainer, R.string.pref_value_book_details_encoding_detected),
                Pair(usedEncodingContainer, R.string.pref_value_book_details_encoding_used),
                Pair(lastActionContainer, R.string.pref_value_book_details_last_action),
                Pair(noteCountContainer, R.string.pref_value_book_details_notes_count)
        )

        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        override fun onClick(v: View) {
            adapterPosition.let { position ->
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onClick(v, position, getItem(position))
                } else {
                    Log.e(TAG, "Adapter position for $v not available")
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            adapterPosition.let { position ->
                return if (position != RecyclerView.NO_POSITION) {
                    clickListener.onLongClick(v, position, getItem(position))
                    true
                } else {
                    Log.e(TAG, "Adapter position for $v not available")
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_book, parent, false)

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        with(holder) {
            val context = itemView.context

            /*
             * If title exists - use title and set book's name as a subtitle.
             * If title does no exist - use book's name hide the subtitle.
             */
            if (item.book.title != null) {
                title.text = item.book.title
                subTitle.text = item.book.name
                subTitle.visibility = View.VISIBLE
            } else {
                title.text = item.book.name
                subTitle.visibility = View.GONE
            }

            /* Out-of-sync and failed-sync flags. */
            if (item.book.lastAction?.type == BookAction.Type.ERROR) {
                syncFailed.visibility = View.VISIBLE
                syncNeeded.visibility = View.GONE
            } else {
                if (item.isOutOfSync()) {
                    syncNeeded.visibility = View.VISIBLE
                } else {
                    syncNeeded.visibility = View.GONE
                }
                syncFailed.visibility = View.GONE
            }

            val bookDetails = BookDetails(containerToPreference)

            /* Modification time. */
            bookDetails.display(mtimeContainer, true, false) {
                if (item.book.mtime != null && item.book.mtime > 0) {
                    mtime.text = timeString(context, item.book.mtime)
                } else {
                    mtime.text = context.getString(R.string.not_modified)
                }
            }

            bookDetails.display(linkContainer, item.hasLink(), false) {
                link.text = item.linkedTo
            }

            bookDetails.display(syncedToUrlContainer,item.hasSync(), false) {
                syncedToUrl.text = item.syncedTo?.uri.toString()
            }

            bookDetails.display(syncedToMtimeContainer, false, item.hasSync()) {
                syncedToMtime.text = timeString(itemView.context, item.syncedTo?.mtime) ?: "N/A"
            }

            bookDetails.display(syncedToRevisionContainer, item.hasSync(), false) {
                syncedToRevision.text = item.syncedTo?.revision ?: "N/A"
            }

            bookDetails.display(selectedEncodingContainer, item.book.selectedEncoding != null, false) {
                selectedEncoding.text = context.getString(
                        R.string.argument_selected,
                        item.book.selectedEncoding
                )
            }

            bookDetails.display(detectedEncodingContainer, item.book.detectedEncoding != null, false) {
                detectedEncoding.text = context.getString(
                        R.string.argument_detected,
                        item.book.detectedEncoding
                )
            }

            bookDetails.display(usedEncodingContainer, item.book.usedEncoding != null, false) {
                usedEncoding.text = context.getString(
                        R.string.argument_used,
                        item.book.usedEncoding
                )
            }

            /* Always show actions which are not INFO. */
            bookDetails.display(lastActionContainer, item.book.lastAction != null, !lastActionWasInfo(item.book)) {
                lastAction.text = getLastActionText(context, item.book)
            }

            bookDetails.display(noteCountContainer, true, false) {
                if (item.noteCount > 0) {
                    noteCount.text = context.resources.getQuantityString(
                            R.plurals.notes_count_nonzero, item.noteCount, item.noteCount)
                } else {
                    noteCount.text = context.getString(R.string.notes_count_zero)
                }
            }

            if (bookDetails.detailDisplayed) {
                bookDetailsPadding.visibility = View.VISIBLE
            } else {
                bookDetailsPadding.visibility = View.GONE
            }

            /* If it's a dummy book - change opacity. */
            itemView.alpha = if (item.book.isDummy == true) 0.4f else 1f

            getSelection().setIsSelectedBackground(container, item.book.id)
        }
    }

    private fun lastActionWasInfo(book: Book): Boolean {
        return book.lastAction?.type === BookAction.Type.INFO
    }

    private fun getLastActionText(context: Context, book: Book): CharSequence {
        val builder = SpannableStringBuilder()

        builder.append(timeString(context, book.lastAction?.timestamp))
        builder.append(": ")
        val pos = builder.length
        builder.append(book.lastAction?.message)

        if (book.lastAction?.type === BookAction.Type.ERROR) {
            /* Get error color attribute. */
            val arr = context.obtainStyledAttributes(intArrayOf(R.attr.text_error_color))
            val color = arr.getColor(0, 0)
            arr.recycle()

            /* Set error color. */
            builder.setSpan(ForegroundColorSpan(color), pos, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        } else if (book.lastAction?.type === BookAction.Type.PROGRESS) {
            builder.setSpan(StyleSpan(Typeface.BOLD), pos, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return builder
    }

    private fun timeString(context: Context, ts: Long?): String? {
        if (ts == null) {
            return null
        }

        val flags = DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_ABBREV_WEEKDAY

        return DateUtils.formatDateTime(context, ts, flags)
    }

    class BookDetails(private val prefForView: Map<View, Int>) {
        var detailDisplayed = false

        fun display(container: View, display: Boolean, displayEvenIfNotEnabled: Boolean, setValue: () -> Unit) {
            val details = AppPreferences.displayedBookDetails(container.context)
            val pref = prefForView[container]

            if (pref != null && display && (displayEvenIfNotEnabled || details.contains(container.context.getString(pref)))) {
                setValue()
                detailDisplayed = true
                container.visibility = View.VISIBLE

            } else {
                container.visibility = View.GONE
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).book.id
    }

    override fun getSelection(): Selection {
        return adapterSelection
    }

    companion object {
        private val TAG = BooksAdapter::class.java.name

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<BookView> =
                object : DiffUtil.ItemCallback<BookView>() {
                    override fun areItemsTheSame(oldItem: BookView, newItem: BookView): Boolean {
                        return oldItem.book.id == newItem.book.id
                    }

                    override fun areContentsTheSame(oldItem: BookView, newItem: BookView): Boolean {
                        return oldItem == newItem // TODO: Compare content
                    }
                }
    }
}
