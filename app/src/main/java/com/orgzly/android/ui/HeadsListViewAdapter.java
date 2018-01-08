package com.orgzly.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.orgzly.R;
import com.orgzly.android.Note;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.provider.views.DbNoteView;
import com.orgzly.android.ui.util.TitleGenerator;
import com.orgzly.android.ui.views.GesturedListViewItemMenus;
import com.orgzly.android.util.OrgFormatter;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.OrgHead;

public class HeadsListViewAdapter extends SimpleCursorAdapter {
    private static final String TAG = HeadsListViewAdapter.class.getName();

    private final Selection selection;
    private final GesturedListViewItemMenus quickMenu;

    /** Can be in book or search results. */
    private final boolean inBook;
    private TitleGenerator titleGenerator;

    private final UserTimeFormatter userTimeFormatter;

    public HeadsListViewAdapter(Context context, Selection selection, GesturedListViewItemMenus toolbars, boolean inBook) {
        super(context, R.layout.item_head, null, new String[0], new int[0], 0);

        this.selection = selection;
        this.quickMenu = toolbars;
        this.inBook = inBook;

        this.userTimeFormatter = new UserTimeFormatter(context);
        this.titleGenerator = new TitleGenerator(context, inBook, getTitleAttributes());
    }

    private TitleGenerator.TitleAttributes getTitleAttributes() {
        TypedArray typedArray = mContext.obtainStyledAttributes(new int[] {
                R.attr.item_head_state_todo_color,
                R.attr.item_head_state_done_color,
                R.attr.item_head_state_unknown_color,
                R.attr.item_head_post_title_text_size,
                R.attr.item_head_post_title_text_color
        });

        TitleGenerator.TitleAttributes attributes = new TitleGenerator.TitleAttributes(
                typedArray.getColor(0, 0),
                typedArray.getColor(1, 0),
                typedArray.getColor(2, 0),
                typedArray.getDimensionPixelSize(3, 0),
                typedArray.getColor(4, 0));

        typedArray.recycle();

        return attributes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, final Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);

        /* Get or create new view holder. */
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null) {
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        setupMargins(context, holder);

        return view;
    }

    /**
     * Setup margins for different list density options.
     */
    private void setupMargins(Context context, ViewHolder holder) {
        int[] margins = getMarginsForListDensity(context);

        RelativeLayout.LayoutParams payloadParams = (RelativeLayout.LayoutParams) holder.payload.getLayoutParams();
        payloadParams.setMargins(payloadParams.leftMargin, margins[0], payloadParams.rightMargin, 0);

        LinearLayout.LayoutParams[] params = {
                (LinearLayout.LayoutParams) holder.scheduled.getLayoutParams(),
                (LinearLayout.LayoutParams) holder.deadline.getLayoutParams(),
                (LinearLayout.LayoutParams) holder.closed.getLayoutParams(),
                (LinearLayout.LayoutParams) holder.content.getLayoutParams()
        };

        for (LinearLayout.LayoutParams p: params) {
            p.setMargins(0, margins[1], 0, 0);
        }
    }

    protected int[] getMarginsForListDensity(Context context) {
        int itemMargins;
        int belowTitleMargins;

        String density = AppPreferences.notesListDensity(context);

        if (context.getString(R.string.pref_value_list_density_comfortable).equals(density)) { // Comfortable
            itemMargins = (int) context.getResources().getDimension(R.dimen.item_head_padding_comfortable);
            belowTitleMargins = (int) context.getResources().getDimension(R.dimen.item_head_below_title_padding_comfortable);

        } else if (context.getString(R.string.pref_value_list_density_compact).equals(density)) { // Compact
            itemMargins = (int) context.getResources().getDimension(R.dimen.item_head_padding_compact);
            belowTitleMargins = (int) context.getResources().getDimension(R.dimen.item_head_below_title_padding_compact);

        } else  { // Cozy
            itemMargins = (int) context.getResources().getDimension(R.dimen.item_head_padding_cozy);
            belowTitleMargins = (int) context.getResources().getDimension(R.dimen.item_head_below_title_padding_cozy);
        }

        return new int[] { itemMargins, belowTitleMargins };
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        final Note note = NotesClient.fromCursor(cursor);
        final OrgHead head = note.getHead();
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (inBook) {
            if (note.getPosition().getFoldedUnderId() == 0) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        }

        setupIndentContainer(context, holder.indentContainer, inBook ? note.getPosition().getLevel() - 1 : 0);

        updateBullet(context, note, holder);

        if (updateFoldingButton(context, note, holder)) {
            holder.foldButton.setOnClickListener(v -> toggleFoldedState(context, note.getId()));
            holder.bullet.setOnClickListener(v -> toggleFoldedState(context, note.getId()));
        } else {
            holder.foldButton.setOnClickListener(null);
            holder.bullet.setOnClickListener(null);
        }

        /* Book name. */
        if (inBook) {
            holder.bookNameUnderNote.setVisibility(View.GONE);
            holder.bookNameLeftFromNoteText.setVisibility(View.GONE);
        } else {
            switch (Integer.valueOf(AppPreferences.bookNameInSearchResults(context))) {
                case 0:
                    holder.bookNameLeftFromNoteText.setVisibility(View.GONE);
                    holder.bookNameUnderNote.setVisibility(View.GONE);
                    break;
                case 1:
                    holder.bookNameLeftFromNoteText.setText(cursor.getString(cursor.getColumnIndex(DbNoteView.BOOK_NAME)));
                    holder.bookNameLeftFromNoteText.setVisibility(View.VISIBLE);
                    holder.bookNameUnderNote.setVisibility(View.GONE);
                    break;
                case 2:
                    holder.bookNameUnderNoteText.setText(cursor.getString(cursor.getColumnIndex(DbNoteView.BOOK_NAME)));
                    holder.bookNameLeftFromNoteText.setVisibility(View.GONE);
                    holder.bookNameUnderNote.setVisibility(View.VISIBLE);
                    break;
            }
        }

        /* Title. */
        holder.title.setText(titleGenerator.generateTitle(note, head));

        /* Content. */
        if (head.hasContent() && titleGenerator.shouldDisplayContent(note)) {
            if (AppPreferences.isFontMonospaced(context)) {
                holder.content.setTypeface(Typeface.MONOSPACE);
            }

            holder.content.setText(OrgFormatter.INSTANCE.parse(context, head.getContent()));

            holder.content.setVisibility(View.VISIBLE);

        } else {
            holder.content.setVisibility(View.GONE);
        }

        /* Closed time. */
        if (head.hasClosed() && AppPreferences.displayPlanning(context)) {
            holder.closedText.setText(userTimeFormatter.formatAll(head.getClosed()));
            holder.closed.setVisibility(View.VISIBLE);

        } else {
            holder.closed.setVisibility(View.GONE);
        }

        /* see also ListWidgetViewsFactory.setContent */
        /* Deadline time. */
        if (head.hasDeadline() && AppPreferences.displayPlanning(context)) {
            holder.deadlineText.setText(userTimeFormatter.formatAll(head.getDeadline()));
            holder.deadline.setVisibility(View.VISIBLE);

        } else {
            holder.deadline.setVisibility(View.GONE);
        }

        /* Scheduled time. */
        if (head.hasScheduled() && AppPreferences.displayPlanning(context)) {
            holder.scheduledText.setText(userTimeFormatter.formatAll(head.getScheduled()));
            holder.scheduled.setVisibility(View.VISIBLE);

        } else {
            holder.scheduled.setVisibility(View.GONE);
        }

        /* Set alpha for done items. */
        if (head.getState() != null && AppPreferences.doneKeywordsSet(context).contains(head.getState())) {
            holder.payload.setAlpha(0.45f);
        } else {
            holder.payload.setAlpha(1.0f);
        }

        quickMenu.updateView(view, note.getId(), holder.menuContainer, holder.menuFlipper);

        selection.updateView(view, note.getId());
    }

    @SuppressLint("StaticFieldLeak")
    private void toggleFoldedState(final Context context, final long id) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                new Shelf(context).toggleFoldedState(id);
                return null;
            }
        }.execute();
    }

    /**
     * Change folding button appearance.
     */
    private boolean updateFoldingButton(Context context, Note note, ViewHolder holder) {
        boolean isVisible = false;

        if (inBook) {
            boolean contentFoldable = note.getHead().hasContent() &&
                                      AppPreferences.isNotesContentFoldable(context) &&
                                      AppPreferences.isNotesContentDisplayedInList(context);

            if (note.getPosition().getDescendantsCount() > 0 || contentFoldable) {
                isVisible = true;

                /* Type of the fold button. */
                if (note.getPosition().isFolded()) {
                    holder.foldButtonText.setText(R.string.unfold_button_character);
                } else {
                    holder.foldButtonText.setText(R.string.fold_button_character);
                }
            }
        } // else: No indentation (in search results)

        if (isVisible) {
            holder.foldButton.setVisibility(View.VISIBLE);
            holder.foldButtonText.setVisibility(View.VISIBLE);
        } else {
            holder.foldButton.setVisibility(View.INVISIBLE);
            holder.foldButtonText.setVisibility(View.INVISIBLE);
        }

        return isVisible;
    }

    /**
     * Change bullet appearance depending on folding state and number of descendants.
     */
    private void updateBullet(Context context, Note note, ViewHolder holder) {

        if (inBook) {
            if (note.getPosition().getDescendantsCount() > 0) { // Has descendants
                if (note.getPosition().isFolded()) {
                    holder.bullet.setText(R.string.bullet_with_children_folded);
                } else {
                    holder.bullet.setText(R.string.bullet_with_children_unfolded);
                }
                holder.bullet.setTypeface(Typeface.DEFAULT);

            } else { // Has no descendants
                holder.bullet.setText(R.string.bullet);
                holder.bullet.setTypeface(Typeface.DEFAULT_BOLD);
            }

        } else { // No indentation (in search results)
            holder.bullet.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    /**
     * Set indentation views, depending on note level.
     */
    private void setupIndentContainer(Context context, ViewGroup container, int level) {
        if (container.getChildCount() < level) {
            /* We need more lines. */

            /* Make all existing visible. */
            for (int i = 1; i <= container.getChildCount(); i++) {
                container.getChildAt(i - 1).setVisibility(View.VISIBLE);
            }

            /* Inflate the rest. */
            for (int i = container.getChildCount() + 1; i <= level; i++) {
                View.inflate(context, R.layout.indent, container);
            }

        } else if (level < container.getChildCount()) {
            /* Has more lines then needed. */

            /* Make required lines visible. */
            for (int i = 1; i <= level; i++) {
                container.getChildAt(i - 1).setVisibility(View.VISIBLE);
            }

            /* Hide the rest. */
            for (int i = level + 1; i <= container.getChildCount(); i++) {
                container.getChildAt(i - 1).setVisibility(View.GONE);
            }

        } else {
            /* Make all visible. */
            for (int i = 1; i <= container.getChildCount(); i++) {
                container.getChildAt(i - 1).setVisibility(View.VISIBLE);
            }
        }
    }
}

