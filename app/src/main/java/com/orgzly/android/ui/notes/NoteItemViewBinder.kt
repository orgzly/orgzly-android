package com.orgzly.android.ui.notes

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.ImageLoader
import com.orgzly.android.ui.util.TitleGenerator
import com.orgzly.android.usecase.NoteToggleFolding
import com.orgzly.android.usecase.NoteUpdateContent
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.util.UserTimeFormatter

class NoteItemViewBinder(private val context: Context, private val inBook: Boolean) {
    private val attrs: Attrs

    private val titleGenerator: TitleGenerator

    private val userTimeFormatter: UserTimeFormatter

    init {
        attrs = Attrs.obtain(context)

        val titleAttributes = TitleGenerator.TitleAttributes(
                attrs.todoColor,
                attrs.doneColor,
                attrs.unknownColor,
                attrs.postTitleTextSize,
                attrs.postTitleTextColor)

        titleGenerator = TitleGenerator(context, inBook, titleAttributes)

        userTimeFormatter = UserTimeFormatter(context)
    }

    fun bind(holder: NoteItemViewHolder, noteView: NoteView) {

        setupTitle(holder, noteView)

        setupMargins(holder) // TODO: Stop doing this for every note

        setupBookName(holder, noteView)

        setupPlanningTimes(holder, noteView)

        setupContent(holder, noteView.note)

        setupIndent(holder, noteView.note)

        setupBullet(holder, noteView.note)

        setupFoldingButtons(holder, noteView.note)

        setupAlpha(holder, noteView.note)
    }

    private fun setupBookName(holder: NoteItemViewHolder, noteView: NoteView) {
        if (inBook) {
            holder.bookNameUnderNote.visibility = View.GONE
            holder.bookNameLeftFromNoteText.visibility = View.GONE

        } else {
            when (Integer.valueOf(AppPreferences.bookNameInSearchResults(context))) {
                0 -> {
                    holder.bookNameLeftFromNoteText.visibility = View.GONE
                    holder.bookNameUnderNote.visibility = View.GONE
                }

                1 -> {
                    holder.bookNameLeftFromNoteText.text = noteView.bookName
                    holder.bookNameLeftFromNoteText.visibility = View.VISIBLE
                    holder.bookNameUnderNote.visibility = View.GONE
                }

                2 -> {
                    holder.bookNameUnderNoteText.text = noteView.bookName
                    holder.bookNameLeftFromNoteText.visibility = View.GONE
                    holder.bookNameUnderNote.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupTitle(holder: NoteItemViewHolder, noteView: NoteView) {
        holder.title.text = titleGenerator.generateTitle(noteView)
    }

    private fun setupContent(holder: NoteItemViewHolder, note: Note) {
        holder.content.text = note.content

        if (note.hasContent() && titleGenerator.shouldDisplayContent(note)) {
            if (AppPreferences.isFontMonospaced(context)) {
                holder.content.typeface = Typeface.MONOSPACE
            }

            holder.content.setRawText(note.content as CharSequence)

            /* If content changes (for example by toggling the checkbox), update the note. */
            holder.content.onUserTextChangeListener = Runnable {
                if (holder.content.getRawText() != null) {
                    val useCase = NoteUpdateContent(
                            note.position.bookId,
                            note.id,
                            holder.content.getRawText()?.toString())

                    App.EXECUTORS.diskIO().execute {
                        UseCaseRunner.run(useCase)
                    }
                }
            }

            ImageLoader.loadImages(holder.content)

            holder.content.visibility = View.VISIBLE

        } else {
            holder.content.visibility = View.GONE
        }
    }

    private fun setupPlanningTimes(holder: NoteItemViewHolder, noteView: NoteView) {
        fun setupPlanningTime(containerView: View, textView: TextView, value: String?) {
            if (value != null && AppPreferences.displayPlanning(context)) {
                val range = com.orgzly.org.datetime.OrgRange.parse(value)
                textView.text = userTimeFormatter.formatAll(range)
                containerView.visibility = View.VISIBLE

            } else {
                containerView.visibility = View.GONE
            }
        }

        setupPlanningTime(holder.scheduled, holder.scheduledText, noteView.scheduledRangeString)
        setupPlanningTime(holder.deadline, holder.deadlineText, noteView.deadlineRangeString)
        setupPlanningTime(holder.event, holder.eventText, noteView.eventString)
        setupPlanningTime(holder.closed, holder.closedText, noteView.closedRangeString)
    }

    private fun setupAlpha(holder: NoteItemViewHolder, note: Note) {
        /* Set alpha for done items. */
        if (note.state != null && AppPreferences.doneKeywordsSet(context).contains(note.state)) {
            holder.payload.alpha = 0.45f
        } else {
            holder.payload.alpha = 1.0f
        }
    }

//    @SuppressWarnings("ResourceType")
//    private fun readAttributes(context: Context) {
//    }

    /**
     * Set indentation views, depending on note level.
     */
    private fun setupIndent(holder: NoteItemViewHolder, note: Note) {
        val container = holder.indentContainer

        val level = if (inBook) note.position.level - 1 else 0

        when {
            container.childCount < level -> {
                /* We need more lines. */

                /* Make all existing visible. */
                for (i in 1..container.childCount) {
                    container.getChildAt(i - 1).visibility = View.VISIBLE
                }

                /* Inflate the rest. */
                for (i in container.childCount + 1..level) {
                    View.inflate(container.context, R.layout.indent, container)
                }
            }

            level < container.childCount -> {
                /* Has more lines then needed. */

                /* Make required lines visible. */
                for (i in 1..level) {
                    container.getChildAt(i - 1).visibility = View.VISIBLE
                }

                /* Hide the rest. */
                for (i in level + 1..container.childCount) {
                    container.getChildAt(i - 1).visibility = View.GONE
                }
            }

            else -> /* Make all visible. */
                for (i in 1..container.childCount) {
                    container.getChildAt(i - 1).visibility = View.VISIBLE
                }
        }
    }

    /**
     * Change bullet appearance depending on folding state and number of descendants.
     */
    private fun setupBullet(holder: NoteItemViewHolder, note: Note) {
        if (inBook) {
            holder.bulletContainer.visibility = View.VISIBLE

            if (note.position.descendantsCount > 0) { // With descendants
                if (note.position.isFolded) { // Folded
                    holder.bullet.setImageDrawable(attrs.bulletFolded)
                } else { // Not folded
                    holder.bullet.setImageDrawable(attrs.bulletUnfolded)
                }
            } else { // No descendants
                holder.bullet.setImageDrawable(attrs.bulletDefault)
            }

        } else {
            holder.bulletContainer.visibility = View.GONE
        }
    }

    private fun setupFoldingButtons(holder: NoteItemViewHolder, note: Note) {
        if (updateFoldingButtons(context, note, holder)) {
            holder.foldButton.setOnClickListener { toggleFoldedState(note.id) }
            holder.bullet.setOnClickListener { toggleFoldedState(note.id) }
        } else {
            holder.foldButton.setOnClickListener(null)
            holder.bullet.setOnClickListener(null)
        }
    }

    /**
     * Change folding button appearance.
     */
    private fun updateFoldingButtons(context: Context, note: Note, holder: NoteItemViewHolder): Boolean {
        var isVisible = false

        if (inBook) {
            val contentFoldable = note.hasContent() &&
                    AppPreferences.isNotesContentFoldable(context) &&
                    AppPreferences.isNotesContentDisplayedInList(context)

            if (note.position.descendantsCount > 0 || contentFoldable) {
                isVisible = true

                /* Type of the fold button. */
                if (note.position.isFolded) {
                    holder.foldButtonText.setText(R.string.unfold_button_character)
                } else {
                    holder.foldButtonText.setText(R.string.fold_button_character)
                }
            }
        }

        if (isVisible) {
            holder.foldButton.visibility = View.VISIBLE
            holder.foldButtonText.visibility = View.VISIBLE
        } else {
            holder.foldButton.visibility = View.INVISIBLE
            holder.foldButtonText.visibility = View.INVISIBLE
        }

        return isVisible
    }


    /**
     * Setup margins for different list density options.
     */
    private fun setupMargins(holder: NoteItemViewHolder) {
        val margins = getMarginsForListDensity(context)

        val payloadParams = holder.payload.layoutParams as RelativeLayout.LayoutParams
        payloadParams.setMargins(payloadParams.leftMargin, margins.first, payloadParams.rightMargin, 0)

        val params = arrayOf(
                holder.scheduled.layoutParams as LinearLayout.LayoutParams,
                holder.deadline.layoutParams as LinearLayout.LayoutParams,
                holder.event.layoutParams as LinearLayout.LayoutParams,
                holder.closed.layoutParams as LinearLayout.LayoutParams,
                holder.content.layoutParams as LinearLayout.LayoutParams)

        for (p in params) {
            p.setMargins(0, margins.second, 0, 0)
        }
    }

    // TODO: Move out
    private fun toggleFoldedState(id: Long) {
        App.EXECUTORS.diskIO().execute {
            UseCaseRunner.run(NoteToggleFolding(id))
        }
    }

    companion object {
        fun getMarginsForListDensity(context: Context): Pair<Int, Int> {
            val itemMargins: Int
            val belowTitleMargins: Int

            val density = AppPreferences.notesListDensity(context)

            val res = context.resources

            when (density) {
                context.getString(R.string.pref_value_list_density_comfortable) -> { // Comfortable
                    itemMargins = res.getDimension(R.dimen.item_head_padding_comfortable).toInt()
                    belowTitleMargins = res.getDimension(R.dimen.item_head_below_title_padding_comfortable).toInt()

                }
                context.getString(R.string.pref_value_list_density_compact) -> { // Compact
                    itemMargins = res.getDimension(R.dimen.item_head_padding_compact).toInt()
                    belowTitleMargins = res.getDimension(R.dimen.item_head_below_title_padding_compact).toInt()

                }
                else -> { // Cozy
                    itemMargins = res.getDimension(R.dimen.item_head_padding_cozy).toInt()
                    belowTitleMargins = res.getDimension(R.dimen.item_head_below_title_padding_cozy).toInt()
                }
            }

            return Pair(itemMargins, belowTitleMargins)
        }
    }


    private data class Attrs(
            @ColorInt val todoColor: Int,
            @ColorInt val doneColor: Int,
            @ColorInt val unknownColor: Int,
            val postTitleTextSize: Int,
            @ColorInt val postTitleTextColor: Int,
            val bulletDefault: Drawable,
            val bulletFolded: Drawable,
            val bulletUnfolded: Drawable
    ) {
        companion object {
            @SuppressWarnings("ResourceType")
            fun obtain(context: Context): Attrs {
                val typedArray = context.obtainStyledAttributes(intArrayOf(
                        R.attr.item_head_state_todo_color,
                        R.attr.item_head_state_done_color,
                        R.attr.item_head_state_unknown_color,
                        R.attr.item_head_post_title_text_size,
                        R.attr.text_secondary_color,
                        R.attr.bullet_default,
                        R.attr.bullet_folded,
                        R.attr.bullet_unfolded))

                val attrs = Attrs(
                        typedArray.getColor(0, 0),
                        typedArray.getColor(1, 0),
                        typedArray.getColor(2, 0),
                        typedArray.getDimensionPixelSize(3, 0),
                        typedArray.getColor(4, 0),
                        typedArray.getDrawable(5)!!,
                        typedArray.getDrawable(6)!!,
                        typedArray.getDrawable(7)!!)

                typedArray.recycle()

                return attrs
            }
        }
    }
}