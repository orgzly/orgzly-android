package com.orgzly.android.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.orgzly.R;

/**
 *
 */
public class ViewHolder /* extends RecyclerView.ViewHolder */ {
    private static final String TAG = ViewHolder.class.getName();

    public View container;

    public TextView bookNameLeftFromNoteText;
    public View bookNameUnderNote;
    public TextView bookNameUnderNoteText;

    public ViewGroup indentContainer;

    public TextView bullet;

    public TextView title;
    public View payload;

    public View scheduled;
    public TextView scheduledText;

    public View deadline;
    public TextView deadlineText;

    public View closed;
    public TextView closedText;

    public TextView content;

    public ViewGroup menuContainer;
    public ViewFlipper menuFlipper;

    public View foldButton;
    public TextView foldButtonText;

    /**
     * Find all views from passed container.
     */
    public ViewHolder(View itemView) {
        // super(itemView);

        container = itemView.findViewById(R.id.item_head_container);

        bookNameLeftFromNoteText = (TextView) itemView.findViewById(R.id.item_head_book_name_before_note_text);
        bookNameUnderNote = itemView.findViewById(R.id.item_head_book_name);
        bookNameUnderNoteText = (TextView) itemView.findViewById(R.id.item_head_book_name_text);

        indentContainer = (ViewGroup) itemView.findViewById(R.id.item_head_indent_container);
        bullet = (TextView) itemView.findViewById(R.id.item_head_bullet);

        payload = itemView.findViewById(R.id.item_head_payload);

        title = (TextView) itemView.findViewById(R.id.item_head_title);

        scheduled = itemView.findViewById(R.id.item_head_scheduled);
        scheduledText = (TextView) itemView.findViewById(R.id.item_head_scheduled_text);

        deadline = itemView.findViewById(R.id.item_head_deadline);
        deadlineText = (TextView) itemView.findViewById(R.id.item_head_deadline_text);

        closed = itemView.findViewById(R.id.item_head_closed);
        closedText = (TextView) itemView.findViewById(R.id.item_head_closed_text);

        content = (TextView) itemView.findViewById(R.id.item_head_content);

        menuContainer = (ViewGroup) itemView.findViewById(R.id.item_head_menu_container);
        menuFlipper = (ViewFlipper) itemView.findViewById(R.id.item_head_menu_flipper);

        foldButton = itemView.findViewById(R.id.item_head_fold_button);
        foldButtonText = (TextView) itemView.findViewById(R.id.item_head_fold_button_text);
    }
}
