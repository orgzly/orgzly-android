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
import com.orgzly.android.usecase.NoteToggleFoldingSubtree
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

    fun bind(holder: NoteItemViewHolder, noteView: NoteView, agendaTimeType: Int? = 0) {

        setupTitle(holder, noteView)

        setupMargins(holder) // TODO: Stop doing this for every note

        setupBookName(holder, noteView)

        setupPlanningTimes(holder, noteView, agendaTimeType)

        setupContent(holder, noteView.note)

        setupIndent(holder, noteView.note)

        setupBullet(holder, noteView.note)

        setupFoldingButtons(holder, noteView.note)

        setupAlpha(holder, noteView.note)
    }

    private fun setupBookName(holder: NoteItemViewHolder, noteView: NoteView) {
        if (inBook) {
            holder.binding.itemHeadBookName.visibility = View.GONE
            holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.GONE

        } else {
            when (Integer.valueOf(AppPreferences.bookNameInSearchResults(context))) {
                0 -> { // Hide
                    holder.binding.itemHeadBookName.visibility = View.GONE

                    holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.GONE
                }

                1 -> { // Show before note
                    holder.binding.itemHeadBookName.visibility = View.GONE

                    holder.binding.itemHeadBookNameBeforeNoteText.text = noteView.bookName
                    holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.VISIBLE
                }

                2 -> { // Show under note
                    holder.binding.itemHeadBookNameText.text = noteView.bookName
                    holder.binding.itemHeadBookName.visibility = View.VISIBLE

                    holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.GONE
                }
            }
        }
    }

    private fun setupTitle(holder: NoteItemViewHolder, noteView: NoteView) {
        holder.binding.itemHeadTitle.text = titleGenerator.generateTitle(noteView)
    }

    fun generateTitle(noteView: NoteView): CharSequence {
        return titleGenerator.generateTitle(noteView)
    }

    private fun setupContent(holder: NoteItemViewHolder, note: Note) {
        holder.binding.itemHeadContent.text = note.content

        if (note.hasContent() && titleGenerator.shouldDisplayContent(note)) {
            if (AppPreferences.isFontMonospaced(context)) {
                holder.binding.itemHeadContent.typeface = Typeface.MONOSPACE
            }

            holder.binding.itemHeadContent.setRawText(note.content as CharSequence)

            /* If content changes (for example by toggling the checkbox), update the note. */
            holder.binding.itemHeadContent.onUserTextChangeListener = Runnable {
                if (holder.binding.itemHeadContent.getRawText() != null) {
                    val useCase = NoteUpdateContent(
                            note.position.bookId,
                            note.id,
                            holder.binding.itemHeadContent.getRawText()?.toString())

                    App.EXECUTORS.diskIO().execute {
                        UseCaseRunner.run(useCase)
                    }
                }
            }

            ImageLoader.loadImages(holder.binding.itemHeadContent)

            holder.binding.itemHeadContent.visibility = View.VISIBLE

        } else {
            holder.binding.itemHeadContent.visibility = View.GONE
        }
    }

    private fun setupPlanningTimes(holder: NoteItemViewHolder, noteView: NoteView, agendaTimeType: Int? = 0) {

        fun setupPlanningTime(containerView: View, textView: TextView, value: String?) {
            if (value != null && AppPreferences.displayPlanning(context)) {
                val range = com.orgzly.org.datetime.OrgRange.parse(value)
                textView.text = userTimeFormatter.formatAll(range)
                containerView.visibility = View.VISIBLE

            } else {
                containerView.visibility = View.GONE
            }
        }

        var scheduled = noteView.scheduledRangeString
        var deadline = noteView.deadlineRangeString
        var event = noteView.eventString

        // In Agenda only display time responsible for item's presence
        when (agendaTimeType) {
            1 -> {
                deadline = null
                event = null
            }
            2 -> {
                scheduled = null
                event = null
            }
            3 -> {
                scheduled = null
                deadline = null
            }
        }

        setupPlanningTime(
                holder.binding.itemHeadScheduled,
                holder.binding.itemHeadScheduledText,
                scheduled)

        setupPlanningTime(
                holder.binding.itemHeadDeadline,
                holder.binding.itemHeadDeadlineText,
                deadline)

        setupPlanningTime(
                holder.binding.itemHeadEvent,
                holder.binding.itemHeadEventText,
                event)

        setupPlanningTime(
                holder.binding.itemHeadClosed,
                holder.binding.itemHeadClosedText,
                noteView.closedRangeString)
    }

    /** Set alpha for done items. */
    private fun setupAlpha(holder: NoteItemViewHolder, note: Note) {
        holder.binding.itemHeadPayload.alpha =
                if (note.state != null && AppPreferences.doneKeywordsSet(context).contains(note.state)) {
                    0.45f
                } else {
                    1.0f
                }
    }

    /**
     * Set indentation views, depending on note level.
     */
    private fun setupIndent(holder: NoteItemViewHolder, note: Note) {
        val container = holder.binding.itemHeadIndentContainer

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

        container.tag = level
    }

    /**
     * Change bullet appearance depending on folding state and number of descendants.
     */
    private fun setupBullet(holder: NoteItemViewHolder, note: Note) {
        if (inBook) {
            holder.binding.itemHeadBulletContainer.visibility = View.VISIBLE

            if (note.position.descendantsCount > 0) { // With descendants
                if (note.position.isFolded) { // Folded
                    holder.binding.itemHeadBullet.setImageDrawable(attrs.bulletFolded)
                } else { // Not folded
                    holder.binding.itemHeadBullet.setImageDrawable(attrs.bulletUnfolded)
                }
            } else { // No descendants
                holder.binding.itemHeadBullet.setImageDrawable(attrs.bulletDefault)
            }

        } else {
            holder.binding.itemHeadBulletContainer.visibility = View.GONE
        }
    }

    private fun setupFoldingButtons(holder: NoteItemViewHolder, note: Note) {
        if (updateFoldingButtons(context, note, holder)) {
            // Folding button
            holder.binding.itemHeadFoldButton.setOnClickListener {
                toggleFoldedState(note.id)
            }
            holder.binding.itemHeadFoldButton.setOnLongClickListener {
                toggleFoldedStateForSubtree(note.id)
            }

            // Bullet
            holder.binding.itemHeadBullet.setOnClickListener {
                toggleFoldedState(note.id)
            }
            holder.binding.itemHeadBullet.setOnLongClickListener {
                toggleFoldedStateForSubtree(note.id)
            }

        } else {
            // Folding button
            holder.binding.itemHeadFoldButton.setOnClickListener(null)
            holder.binding.itemHeadFoldButton.setOnLongClickListener(null)

            // Bullet
            holder.binding.itemHeadBullet.setOnClickListener(null)
            holder.binding.itemHeadBullet.setOnLongClickListener(null)
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
                    holder.binding.itemHeadFoldButtonText.setText(R.string.unfold_button_character)
                } else {
                    holder.binding.itemHeadFoldButtonText.setText(R.string.fold_button_character)
                }
            }
        }

        if (isVisible) {
            holder.binding.itemHeadFoldButton.visibility = View.VISIBLE
            holder.binding.itemHeadFoldButtonText.visibility = View.VISIBLE
        } else {
            if (inBook) { // Leave invisible for padding
                holder.binding.itemHeadFoldButton.visibility = View.INVISIBLE
                holder.binding.itemHeadFoldButtonText.visibility = View.INVISIBLE
            } else {
                holder.binding.itemHeadFoldButton.visibility = View.GONE
                holder.binding.itemHeadFoldButtonText.visibility = View.GONE
            }
        }

        // Add right margin in search results
        if (!inBook) {
            (holder.binding.itemHeadContainer.layoutParams as LinearLayout.LayoutParams).apply {
                val right = if (holder.binding.itemHeadFoldButton.visibility == View.GONE) {
                    context.resources.getDimension(R.dimen.screen_edge).toInt()
                } else {
                    0
                }

                setMargins(leftMargin, topMargin, right, bottomMargin)
            }
        }

        return isVisible
    }


    /**
     * Setup margins for different list density options.
     */
    private fun setupMargins(holder: NoteItemViewHolder) {
        val margins = getMarginsForListDensity(context)

        val payloadParams = holder.binding.itemHeadPayload.layoutParams as RelativeLayout.LayoutParams
        payloadParams.setMargins(payloadParams.leftMargin, margins.first, payloadParams.rightMargin, 0)

        val params = arrayOf(
                holder.binding.itemHeadScheduled.layoutParams as LinearLayout.LayoutParams,
                holder.binding.itemHeadDeadline.layoutParams as LinearLayout.LayoutParams,
                holder.binding.itemHeadEvent.layoutParams as LinearLayout.LayoutParams,
                holder.binding.itemHeadClosed.layoutParams as LinearLayout.LayoutParams,
                holder.binding.itemHeadContent.layoutParams as LinearLayout.LayoutParams)

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

    private fun toggleFoldedStateForSubtree(id: Long): Boolean {
        App.EXECUTORS.diskIO().execute {
            UseCaseRunner.run(NoteToggleFoldingSubtree(id))
        }

        return true
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