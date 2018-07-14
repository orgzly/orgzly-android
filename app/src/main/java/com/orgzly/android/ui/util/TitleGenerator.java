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

import com.orgzly.android.Note;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.OrgFormatter;
import com.orgzly.org.OrgHead;
import com.orgzly.org.datetime.OrgDateTime;

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

    public CharSequence generateTitle(Note note, OrgHead head) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        /* State. */
        if (head.getState() != null) {
            builder.append(generateState(head));
        }

        /* Priority. */
        if (head.getPriority() != null) {
            if (builder.length() > 0) {
                builder.append(TITLE_SEPARATOR);
            }
            builder.append(generatePriority(head));
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
        builder.append(OrgFormatter.parse(head.getTitle(), mContext, true, false));

        /* Append note ID. */
        // builder.append(TITLE_SEPARATOR).append("#").append(String.valueOf(note.getId()));

        int mark = builder.length();

        boolean hasPostTitleText = false;

        /* Tags. */
        if (head.hasTags()) {
            builder.append(TITLE_SEPARATOR).append(generateTags(head.getTags()));
            hasPostTitleText = true;
        }

        /* Inherited tags in search results. */
        if (!inBook && note.hasInheritedTags() && AppPreferences.inheritedTagsInSearchResults(mContext)) {
            if (head.hasTags()) {
                builder.append(INHERITED_TAGS_SEPARATOR);
            } else {
                builder.append(TITLE_SEPARATOR);
            }
            builder.append(generateTags(note.getInheritedTags()));
            hasPostTitleText = true;
        }

        /* Content line number. */
        if (head.hasContent() && AppPreferences.contentLineCountDisplayed(mContext)) {
            if (!shouldDisplayContent(note)) {
                builder.append(TITLE_SEPARATOR).append(String.valueOf(note.getContentLines()));
                hasPostTitleText = true;
            }
        }

        if (false) {
            String times = note.getCreatedAt() > 0
                    ? new OrgDateTime(note.getCreatedAt(), false).toString()
                    : "N/A";
            builder.append("  ").append(times);
            hasPostTitleText = true;
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

    private CharSequence generateState(OrgHead head) {
        SpannableString str = new SpannableString(head.getState());

        ForegroundColorSpan color;

        if (AppPreferences.todoKeywordsSet(mContext).contains(head.getState())) {
            color = attributes.colorTodo;
        } else if (AppPreferences.doneKeywordsSet(mContext).contains(head.getState())) {
            color = attributes.colorDone;
        } else {
            color = attributes.colorUnknown;
        }

        str.setSpan(color, 0, str.length(), 0);

        return str;
    }

    private CharSequence generatePriority(OrgHead head) {
        return "#" + head.getPriority();
    }

    public static class TitleAttributes {
        private ForegroundColorSpan colorTodo;
        private ForegroundColorSpan colorDone;
        private ForegroundColorSpan colorUnknown;
        private AbsoluteSizeSpan postTitleTextSize;
        private ForegroundColorSpan postTitleTextColor;

        public TitleAttributes(int colorTodo, int colorDone, int colorUnknown, int postTitleTextSize, int postTitleTextColor) {
            this.colorTodo = new ForegroundColorSpan(colorTodo);
            this.colorDone = new ForegroundColorSpan(colorDone);
            this.colorUnknown = new ForegroundColorSpan(colorUnknown);
            this.postTitleTextSize = new AbsoluteSizeSpan(postTitleTextSize);
            this.postTitleTextColor = new ForegroundColorSpan(postTitleTextColor);
        }
    }
}
