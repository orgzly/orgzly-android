package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.util.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Book's preface and settings.
 */
public class BookEditorFragment extends Fragment {
    private static final String TAG = BookEditorFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = BookEditorFragment.class.getName();

    private static final String ARG_BOOK_ID = "book_id";
    private static final String ARG_BOOK_PREFACE = "book_preface";

    private long bookId;
    private Book book;
    private EditText contentView;

    private EditorListener listener;

    public static BookEditorFragment getInstance(long bookId, String bookPreface) {
        BookEditorFragment fragment = new BookEditorFragment();

        /* Set arguments for a fragment. */
        Bundle args = new Bundle();
        args.putLong(BookEditorFragment.ARG_BOOK_ID, bookId);
        args.putString(BookEditorFragment.ARG_BOOK_PREFACE, bookPreface);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BookEditorFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }

    @Override
    public void onAttach(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getActivity());
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            listener = (EditorListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + EditorListener.class);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View top = inflater.inflate(R.layout.fragment_book_editor, container, false);

        contentView = (EditText) top.findViewById(R.id.fragment_editor_container_edit);

        if (getActivity() != null && AppPreferences.isFontMonospaced(getContext())) {
            contentView.setTypeface(Typeface.MONOSPACE);
        }

        /* Parse arguments - set content. */
        if (getArguments() == null) {
            throw new IllegalArgumentException(BookEditorFragment.class.getSimpleName() + " has no arguments passed");
        }

        if (!getArguments().containsKey(ARG_BOOK_ID)) {
            throw new IllegalArgumentException(BookEditorFragment.class.getSimpleName() + " has no book id passed");
        }

        if (!getArguments().containsKey(ARG_BOOK_PREFACE)) {
            throw new IllegalArgumentException(BookEditorFragment.class.getSimpleName() + " has no book preface passed");
        }

        bookId = getArguments().getLong(ARG_BOOK_ID);
        book = BooksClient.get(getActivity(), bookId);
        contentView.setText(getArguments().getString(ARG_BOOK_PREFACE));

        return top;
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        announceChangesToActivity();
    }

    private void announceChangesToActivity() {
        if (listener != null) {
            listener.announceChanges(
                    BookEditorFragment.FRAGMENT_TAG,
                    Book.getFragmentTitleForBook(book),
                    Book.getFragmentSubtitleForBook(book),
                    0);
        }
    }

    @Override
    public void onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDetach();

        listener = null;
    }

	/*
	 * Options Menu.
	 */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.done_or_close, menu);

        /* Remove search item. */
        menu.removeItem(R.id.activity_action_search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item);

        switch (item.getItemId()) {
            case R.id.close:
                listener.onBookPrefaceEditCancelRequest();
                return true;

            case R.id.done:
                save();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void save() {
        updateBookFromViews(book);
        listener.onBookPrefaceEditSaveRequest(book);
    }

    private void updateBookFromViews(Book book) {
        book.setPreface(contentView.getText().toString());

        parsePrefaceForInBufferSettings(book, contentView.getText().toString());
    }

    private void parsePrefaceForInBufferSettings(Book book, String preface) {
        /* Set default values for settings that could be overwritten. */
        book.getOrgFileSettings().setTitle(null);

        BufferedReader reader = new BufferedReader(new StringReader(preface));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                book.getOrgFileSettings().parseLine(line);
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface EditorListener extends FragmentListener {
        void onBookPrefaceEditSaveRequest(Book book);
        void onBookPrefaceEditCancelRequest();
    }
}
