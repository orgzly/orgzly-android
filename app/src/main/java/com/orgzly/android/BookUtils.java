package com.orgzly.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.orgzly.R;
import com.orgzly.android.prefs.AppPreferences;

import java.io.IOException;
import java.util.List;

public class BookUtils {
    /**
     * Returns either notebook's #+TITLE or file name.
     */
    public static String getFragmentTitleForBook(Book book) {
        String str = null;

        if (book != null) {
            if (book.getOrgFileSettings().getTitle() != null) {
                str = book.getOrgFileSettings().getTitle();
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
            if (book.getOrgFileSettings().getTitle() != null) {
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

    /**
     * Returns default book if it exists, or first one found.
     * If there are no books, default book will be created.
     */
    public static Book getTargetBook(Context context) throws IOException {
        Shelf shelf = new Shelf(context);
        List<Book> books = shelf.getBooks();
        String defaultBookName = AppPreferences.shareNotebook(context);

        if (books.size() == 0) {
            return shelf.createBook(defaultBookName);
        } else {
            for (Book book : books) {
                if (defaultBookName.equals(book.getName())) {
                    return book;
                }
            }
            return books.get(0);
        }
    }
}
