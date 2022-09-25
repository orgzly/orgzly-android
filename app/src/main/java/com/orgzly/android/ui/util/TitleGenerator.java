package com.orgzly.android.ui.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.orgzly.android.db.entity.Note;
import com.orgzly.android.db.entity.NoteView;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.OrgFormatter;

import java.util.List;

public class TitleGenerator {
    /* Separator for heading parts (state, priority, title, tags). */
    private static final String TITLE_SEPARATOR = "  ";

    /*
     * Separator between note's tags and inherited tags.
     * Not used if note doesn't have its own tags.
     */
    private static final String INHERITED_TAGS_SEPARATOR = " • ";

    /* Separator for individual tags. */
    private static final String TAGS_SEPARATOR = " ";

    private Context mContext;
    /** Can be in book or search results. */
    private boolean inBook;
    private TitleAttributes attributes;

    public TitleGenerator(Context mContext, boolean inBook, TitleAttributes attributes) {
        this.mContext = mContext;
        this.inBook = inBook;
        this.attributes = attributes;
    }

    public CharSequence generateTitle(NoteView noteView) {
        Note note = noteView.getNote();

        SpannableStringBuilder builder = new SpannableStringBuilder();

        /* State. */
        if (note.getState() != null) {
            builder.append(generateState(note));
        }

        /* Priority. */
        if (note.getPriority() != null) {
            if (builder.length() > 0) {
                builder.append(TITLE_SEPARATOR);
            }
            builder.append(generatePriority(note));
        }

        /* Bold everything up until now. */
        if (builder.length() > 0) {
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        /* Space before title, unless there's nothing added. */
        if (builder.length() > 0) {
            builder.append(TITLE_SEPARATOR);
        }

        /* Title. */
        builder.append(OrgFormatter.parse(note.getTitle(), mContext, true, false));

        /* Append note ID. */
        // builder.append(TITLE_SEPARATOR).append("#").append(String.valueOf(note.getId()));

        int mark = builder.length();

        boolean hasPostTitleText = false;

        /* Tags. */
        if (note.hasTags()) {
            builder.append(TITLE_SEPARATOR).append(generateTags(note.getTagsList()));
            hasPostTitleText = true;
        }

        /* Inherited tags in search results. */
        if (!inBook && noteView.hasInheritedTags() && AppPreferences.inheritedTagsInSearchResults(mContext)) {
            if (note.hasTags()) {
                builder.append(INHERITED_TAGS_SEPARATOR);
            } else {
                builder.append(TITLE_SEPARATOR);
            }
            builder.append(generateTags(noteView.getInheritedTagsList()));
            hasPostTitleText = true;
        }

        /* Content line number. */
        if (note.hasContent() && AppPreferences.contentLineCountDisplayed(mContext)) {
            if (!shouldDisplayContent(note)) {
                builder.append(TITLE_SEPARATOR).append(String.valueOf(note.getContentLineCount()));
                hasPostTitleText = true;
            }
        }

        /* Change font style of text after title. */
        if (hasPostTitleText) {
            builder.setSpan(attributes.postTitleTextSize, mark, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(attributes.postTitleTextColor, mark, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }

    /**
     * Should note's content be displayed if it exists.
     */
    public boolean shouldDisplayContent(Note note) {
        boolean display = true;

        if (AppPreferences.isNotesContentDisplayedInList(mContext)) { // Content could be displayed in list
            if (inBook) { // In book, folded
                if (AppPreferences.isNotesContentFoldable(mContext) && note.getPosition().isFolded()) {
                    display = false;
                }
            } else { // In search results, not displaying content
                if (!AppPreferences.isNotesContentDisplayedInSearch(mContext)) {
                    display = false;
                }
            }

        } else { // Never displaying content in list
            display = false;
        }

        return display;
    }

    private CharSequence generateTags(List<String> tags) {
        return new SpannableString(TextUtils.join(TAGS_SEPARATOR, tags));
    }

    private CharSequence generateState(Note note) {
        SpannableString str = new SpannableString(note.getState());

        ForegroundColorSpan color;

        if (AppPreferences.doneKeywordsSet(mContext).contains(note.getState())) {
            color = attributes.colorDone;
        } else {
            color = attributes.colorTodo;
        }

        str.setSpan(color, 0, str.length(), 0);

        return str;
    }

    private CharSequence generatePriority(Note note) {
        return "#" + note.getPriority();
    }

    public static class TitleAttributes {
        private final ForegroundColorSpan colorTodo;
        private final ForegroundColorSpan colorDone;
        private final AbsoluteSizeSpan postTitleTextSize;
        private final ForegroundColorSpan postTitleTextColor;

        public TitleAttributes(int colorTodo, int colorDone, int postTitleTextSize, int postTitleTextColor) {
            this.colorTodo = new ForegroundColorSpan(colorTodo);
            this.colorDone = new ForegroundColorSpan(colorDone);
            this.postTitleTextSize = new AbsoluteSizeSpan(postTitleTextSize);
            this.postTitleTextColor = new ForegroundColorSpan(postTitleTextColor);
        }
    }
}
