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
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.R
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookAction
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.databinding.ItemBookBinding


class BooksAdapter(
        private val clickListener: OnViewHolderClickListener<BookView>
) : ListAdapter<BookView, BooksAdapter.ViewHolder>(DIFF_CALLBACK), SelectableItemAdapter {

    private val adapterSelection: Selection = Selection()

    inner class ViewHolder(val binding: ItemBookBinding) :
            RecyclerView.ViewHolder(binding.root),
            View.OnClickListener,
            View.OnLongClickListener {

        val containerToPreference = mapOf<View, Int>(
                Pair(binding.itemBookMtimeContainer, R.string.pref_value_book_details_mtime),
                Pair(binding.itemBookLinkContainer, R.string.pref_value_book_details_link_url),
                Pair(binding.itemBookSyncedUrlContainer, R.string.pref_value_book_details_sync_url),
                Pair(binding.itemBookSyncedMtimeContainer, R.string.pref_value_book_details_sync_mtime),
                Pair(binding.itemBookSyncedRevisionContainer, R.string.pref_value_book_details_sync_revision),
                Pair(binding.itemBookEncodingSelectedContainer, R.string.pref_value_book_details_encoding_selected),
                Pair(binding.itemBookEncodingDetectedContainer, R.string.pref_value_book_details_encoding_detected),
                Pair(binding.itemBookEncodingUsedContainer, R.string.pref_value_book_details_encoding_used),
                Pair(binding.itemBookLastActionContainer, R.string.pref_value_book_details_last_action),
                Pair(binding.itemBookNoteCountContainer, R.string.pref_value_book_details_notes_count)
        )

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
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
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
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
                binding.itemBookTitle.text = item.book.title
                binding.itemBookSubtitle.text = item.book.name
                binding.itemBookSubtitle.visibility = View.VISIBLE
            } else {
                binding.itemBookTitle.text = item.book.name
                binding.itemBookSubtitle.visibility = View.GONE
            }

            /* Out-of-sync and failed-sync flags. */
            if (item.book.lastAction?.type == BookAction.Type.ERROR) {
                binding.itemBookSyncFailedIcon.visibility = View.VISIBLE
                binding.itemBookSyncNeededIcon.visibility = View.GONE
            } else {
                if (item.isOutOfSync()) {
                    binding.itemBookSyncNeededIcon.visibility = View.VISIBLE
                } else {
                    binding.itemBookSyncNeededIcon.visibility = View.GONE
                }
                binding.itemBookSyncFailedIcon.visibility = View.GONE
            }

            val bookDetails = BookDetails(containerToPreference)

            /* Modification time. */
            bookDetails.display(binding.itemBookMtimeContainer, true, false) {
                if (item.book.mtime != null && item.book.mtime > 0) {
                    binding.itemBookMtime.text = timeString(context, item.book.mtime)
                } else {
                    binding.itemBookMtime.text = context.getString(R.string.not_modified)
                }
            }

            bookDetails.display(binding.itemBookLinkContainer, item.hasLink(), false) {
                binding.itemBookLinkRepo.text = item.linkRepo?.url
            }

            bookDetails.display(binding.itemBookSyncedUrlContainer,item.hasSync(), false) {
                binding.itemBookSyncedUrl.text = item.syncedTo?.uri.toString()
            }

            bookDetails.display(binding.itemBookSyncedMtimeContainer, item.hasSync(), false) {
                binding.itemBookSyncedMtime.text = timeString(itemView.context, item.syncedTo?.mtime) ?: "N/A"
            }

            bookDetails.display(binding.itemBookSyncedRevisionContainer, item.hasSync(), false) {
                binding.itemBookSyncedRevision.text = item.syncedTo?.revision ?: "N/A"
            }

            bookDetails.display(binding.itemBookEncodingSelectedContainer, item.book.selectedEncoding != null, false) {
                binding.itemBookEncodingSelected.text = context.getString(
                        R.string.argument_selected,
                        item.book.selectedEncoding
                )
            }

            bookDetails.display(binding.itemBookEncodingDetectedContainer, item.book.detectedEncoding != null, false) {
                binding.itemBookEncodingDetected.text = context.getString(
                        R.string.argument_detected,
                        item.book.detectedEncoding
                )
            }

            bookDetails.display(binding.itemBookEncodingUsedContainer, item.book.usedEncoding != null, false) {
                binding.itemBookEncodingUsed.text = context.getString(
                        R.string.argument_used,
                        item.book.usedEncoding
                )
            }

            /* Always show actions which are not INFO. */
            bookDetails.display(binding.itemBookLastActionContainer, item.book.lastAction != null, !lastActionWasInfo(item.book)) {
                binding.itemBookLastAction.text = getLastActionText(context, item.book)
            }

            bookDetails.display(binding.itemBookNoteCountContainer, true, false) {
                if (item.noteCount > 0) {
                    binding.itemBookNoteCount.text = context.resources.getQuantityString(
                            R.plurals.notes_count_nonzero, item.noteCount, item.noteCount)
                } else {
                    binding.itemBookNoteCount.text = context.getString(R.string.notes_count_zero)
                }
            }

            if (bookDetails.detailDisplayed) {
                binding.itemBookDetailsPadding.visibility = View.VISIBLE
            } else {
                binding.itemBookDetailsPadding.visibility = View.GONE
            }

            /* If it's a dummy book - change opacity. */
            itemView.alpha = if (item.book.isDummy == true) 0.4f else 1f

            getSelection().setIsSelectedBackground(binding.itemBookContainer, item.book.id)
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
            val color = context.styledAttributes(intArrayOf(R.attr.text_error_color)) { typedArray ->
                typedArray.getColor(0, 0)
            }

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
                        return oldItem == newItem
                    }
                }
    }
}
