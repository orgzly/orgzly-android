package com.orgzly.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.orgzly.R;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.db.entity.BookAction;

public class BookUtils {
    public static final String TAG = BookUtils.class.getName();

    /**
     * Returns either notebook's #+TITLE or file name.
     */
    public static String getFragmentTitleForBook(Book book) {
        String str = null;

        if (book != null) {
            if (book.getTitle() != null) {
                str = book.getTitle();
            } else {
                str = book.getName();
            }
        }

        return str;
    }

    /**
     * Returns book's file name if #+TITLE is set, null otherwise.
     * If book's last action failed, return the error message.
     */
    public static CharSequence getFragmentSubtitleForBook(Context context, Book book) {
        CharSequence str = null;

        if (book != null) {
            if (book.getTitle() != null) {
                str = book.getName();
            }

            str = replaceWithLastActionError(context, book, str);
        }

        return str;
    }

    private static CharSequence replaceWithLastActionError(Context context, Book book, CharSequence str) {
        BookAction action = book.getLastAction();

        if (action != null && action.getType() == BookAction.Type.ERROR) {
            SpannableStringBuilder builder = new SpannableStringBuilder(action.getMessage());

            /* Get error color attribute. */
            TypedArray arr = context.obtainStyledAttributes(
                    new int[]{R.attr.text_error_color});
            int color = arr.getColor(0, 0);
            arr.recycle();

            /* Set error color. */
            builder.setSpan(new ForegroundColorSpan(color), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return builder;
        }

        return str;
    }
}
