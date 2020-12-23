package com.orgzly.android.ui.notes

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.AttachmentSpanLoader
import com.orgzly.android.ui.ImageLoader
import com.orgzly.android.ui.TimeType
import com.orgzly.android.ui.util.TitleGenerator
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.usecase.NoteToggleFolding
import com.orgzly.android.usecase.NoteToggleFoldingSubtree
import com.orgzly.android.usecase.NoteUpdateContent
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.databinding.ItemAgendaDividerBinding
import com.orgzly.databinding.ItemHeadBinding

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

    fun bind(holder: NoteItemViewHolder, noteView: NoteView, agendaTimeType: TimeType? = null) {

        setupTitle(holder, noteView)

        setupBookName(holder, noteView)

        setupPlanningTimes(holder, noteView, agendaTimeType)

        setupContent(holder, noteView.note)

        setupIndent(holder, noteView.note)

        setupBullet(holder, noteView.note)

        setupFoldingButtons(holder, noteView.note)

        setupAlpha(holder, noteView)
    }

    private fun setupBookName(holder: NoteItemViewHolder, noteView: NoteView) {
        if (inBook) {
            holder.binding.itemHeadBookNameIcon.visibility = View.GONE
            holder.binding.itemHeadBookNameText.visibility = View.GONE
            holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.GONE

        } else {
            when (Integer.valueOf(AppPreferences.bookNameInSearchResults(context))) {
                0 -> { // Hide
                    holder.binding.itemHeadBookNameIcon.visibility = View.GONE
                    holder.binding.itemHeadBookNameText.visibility = View.GONE

                    holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.GONE
                }

                1 -> { // Show before note
                    holder.binding.itemHeadBookNameIcon.visibility = View.GONE
                    holder.binding.itemHeadBookNameText.visibility = View.GONE

                    holder.binding.itemHeadBookNameBeforeNoteText.text = noteView.bookName
                    holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.VISIBLE
                }

                2 -> { // Show under note
                    holder.binding.itemHeadBookNameText.text = noteView.bookName
                    holder.binding.itemHeadBookNameText.visibility = View.VISIBLE
                    holder.binding.itemHeadBookNameIcon.visibility = View.VISIBLE

                    holder.binding.itemHeadBookNameBeforeNoteText.visibility = View.GONE
                }
            }
        }
    }

    private fun setupTitle(holder: NoteItemViewHolder, noteView: NoteView) {
        holder.binding.itemHeadTitle.text = generateTitle(noteView)
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

            AttachmentSpanLoader.loadAttachmentPaths(note.id, holder.binding.itemHeadContent)
            ImageLoader.loadImages(holder.binding.itemHeadContent)

            holder.binding.itemHeadContent.visibility = View.VISIBLE

        } else {
            holder.binding.itemHeadContent.visibility = View.GONE
        }
    }

    private fun setupPlanningTimes(holder: NoteItemViewHolder, noteView: NoteView, agendaTimeType: TimeType?) {

        fun setupPlanningTime(textView: TextView, iconView: ImageView, value: String?) {
            if (value != null && AppPreferences.displayPlanning(context)) {
                val range = com.orgzly.org.datetime.OrgRange.parse(value)
                textView.text = userTimeFormatter.formatAll(range)
                textView.visibility = View.VISIBLE
                iconView.visibility = View.VISIBLE

            } else {
                textView.visibility = View.GONE
                iconView.visibility = View.GONE
            }
        }

        var scheduled = noteView.scheduledRangeString
        var deadline = noteView.deadlineRangeString
        var event = noteView.eventString

        // In Agenda only display time responsible for item's presence
        when (agendaTimeType) {
            TimeType.SCHEDULED -> {
                deadline = null
                event = null
            }
            TimeType.DEADLINE -> {
                scheduled = null
                event = null
            }
            TimeType.EVENT -> {
                scheduled = null
                deadline = null
            }
            else -> {
            }
        }

        setupPlanningTime(
                holder.binding.itemHeadScheduledText,
                holder.binding.itemHeadScheduledIcon,
                scheduled)

        setupPlanningTime(
                holder.binding.itemHeadDeadlineText,
                holder.binding.itemHeadDeadlineIcon,
                deadline)

        setupPlanningTime(
                holder.binding.itemHeadEventText,
                holder.binding.itemHeadEventIcon,
                event)

        setupPlanningTime(
                holder.binding.itemHeadClosedText,
                holder.binding.itemHeadClosedIcon,
                noteView.closedRangeString)
    }

    /** Set alpha for done and archived items. */
    private fun setupAlpha(holder: NoteItemViewHolder, noteView: NoteView) {
        val state = noteView.note.state
        val tags = noteView.note.getTagsList()
        val inheritedTags = noteView.getInheritedTagsList()

        val isDone = state != null && AppPreferences.doneKeywordsSet(context).contains(state)
        val isArchived = tags.contains(ARCHIVE_TAG) || inheritedTags.contains(ARCHIVE_TAG)

        holder.binding.alpha =
                if (isDone || isArchived) {
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
            container.childCount < level -> { // More levels needed
                // Make all existing levels visible
                for (i in 1..container.childCount) {
                    container.getChildAt(i - 1).visibility = View.VISIBLE
                }

                // Inflate the rest
                for (i in container.childCount + 1..level) {
                    View.inflate(container.context, R.layout.indent, container)
                }
            }

            level < container.childCount -> { // Too many levels
                // Make required levels visible
                for (i in 1..level) {
                    container.getChildAt(i - 1).visibility = View.VISIBLE
                }

                // Hide the rest
                for (i in level + 1..container.childCount) {
                    container.getChildAt(i - 1).visibility = View.GONE
                }
            }

            else -> // Make all visible
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
            if (note.position.descendantsCount > 0) { // With descendants
                if (note.position.isFolded) { // Folded
                    holder.binding.itemHeadBullet.setImageDrawable(attrs.bulletFolded)
                } else { // Not folded
                    holder.binding.itemHeadBullet.setImageDrawable(attrs.bulletUnfolded)
                }
            } else { // No descendants
                holder.binding.itemHeadBullet.setImageDrawable(attrs.bulletDefault)
            }

            holder.binding.itemHeadBullet.visibility = View.VISIBLE

        } else {
            holder.binding.itemHeadBullet.visibility = View.GONE
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

        // Add horizontal padding when in search results (no bullet, no folding button)
        val horizontalPadding = context.resources.getDimension(R.dimen.screen_edge).toInt()
        if (!inBook) {
            holder.binding.itemHeadContainer.setPadding(
                    horizontalPadding,
                    holder.binding.itemHeadContainer.paddingTop,
                    horizontalPadding,
                    holder.binding.itemHeadContainer.paddingBottom)

//            (holder.binding.itemHeadContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
//                val right = if (holder.binding.itemHeadFoldButtonText.visibility == View.GONE) {
//                    context.resources.getDimension(R.dimen.screen_edge).toInt()
//                } else {
//                    0
//                }
//
//                setMargins(leftMargin, topMargin, right, bottomMargin)
//            }
        } else {
            holder.binding.itemHeadContainer.setPadding(
                    horizontalPadding,
                    holder.binding.itemHeadContainer.paddingTop,
                    holder.binding.itemHeadContainer.paddingRight,
                    holder.binding.itemHeadContainer.paddingBottom)
        }

        return isVisible
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
        const val ARCHIVE_TAG = "ARCHIVE"

        /**
         * Setup margins or padding for different list density settings.
         */
        fun setupSpacingForDensitySetting(context: Context, binding: ItemAgendaDividerBinding) {
            val margins = getMarginsForListDensity(context)

            binding.root.setPadding(
                    binding.root.paddingLeft,
                    margins.first,
                    binding.root.paddingRight,
                    margins.first)

        }
        fun setupSpacingForDensitySetting(context: Context, binding: ItemHeadBinding) {
            val margins = getMarginsForListDensity(context)

            // Whole item margins
            listOf(binding.itemHeadTop, binding.itemHeadBottom).forEach {
                (it.layoutParams as ConstraintLayout.LayoutParams).apply {
                    height = margins.first
                }
            }

            // Spacing for views inside the item
            val views = arrayOf(
                    binding.itemHeadScheduledText,
                    binding.itemHeadScheduledIcon,
                    binding.itemHeadDeadlineText,
                    binding.itemHeadDeadlineIcon,
                    binding.itemHeadEventText,
                    binding.itemHeadEventIcon,
                    binding.itemHeadClosedIcon,
                    binding.itemHeadClosedText,
                    binding.itemHeadContent)

            for (view in views) {
                (view.layoutParams as ConstraintLayout.LayoutParams).apply {
                    setMargins(leftMargin, margins.second, rightMargin, bottomMargin)
                }
            }
        }

        private fun getMarginsForListDensity(context: Context): Pair<Int, Int> {
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
                return context.styledAttributes(
                        intArrayOf(
                                R.attr.item_head_state_todo_color,
                                R.attr.item_head_state_done_color,
                                R.attr.item_head_state_unknown_color,
                                R.attr.item_head_post_title_text_size,
                                R.attr.item_head_post_title_color,
                                R.attr.bullet_default,
                                R.attr.bullet_folded,
                                R.attr.bullet_unfolded)) { typedArray ->

                    Attrs(
                            typedArray.getColor(0, 0),
                            typedArray.getColor(1, 0),
                            typedArray.getColor(2, 0),
                            typedArray.getDimensionPixelSize(3, 0),
                            typedArray.getColor(4, 0),
                            typedArray.getDrawable(5)!!,
                            typedArray.getDrawable(6)!!,
                            typedArray.getDrawable(7)!!)
                }
            }
        }
    }
}