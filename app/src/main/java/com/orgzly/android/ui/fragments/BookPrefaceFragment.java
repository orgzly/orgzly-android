package com.orgzly.android.ui.fragments;

import android.app.Activity;
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
import com.orgzly.android.BookUtils;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Book's preface and settings.
 */
public class BookPrefaceFragment extends Fragment {
    private static final String TAG = BookPrefaceFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = BookPrefaceFragment.class.getName();

    private static final String ARG_BOOK_ID = "book_id";
    private static final String ARG_BOOK_PREFACE = "book_preface";

    private long bookId;
    private Book book;
    private EditText contentView;

    private EditorListener listener;

    public static BookPrefaceFragment getInstance(long bookId, String bookPreface) {
        BookPrefaceFragment fragment = new BookPrefaceFragment();

        /* Set arguments for a fragment. */
        Bundle args = new Bundle();
        args.putLong(BookPrefaceFragment.ARG_BOOK_ID, bookId);
        args.putString(BookPrefaceFragment.ARG_BOOK_PREFACE, bookPreface);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BookPrefaceFragment() {
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

        View top = inflater.inflate(R.layout.fragment_book_preface, container, false);

        contentView = (EditText) top.findViewById(R.id.fragment_book_preface_content);

        Activity activity = getActivity();

        if (activity != null && AppPreferences.isFontMonospaced(getContext())) {
            contentView.setTypeface(Typeface.MONOSPACE);
        }

        // Open keyboard
        if (activity != null) {
            ActivityUtils.openSoftKeyboard(activity, contentView);
        }

        /* Parse arguments - set content. */
        if (getArguments() == null) {
            throw new IllegalArgumentException(BookPrefaceFragment.class.getSimpleName() + " has no arguments passed");
        }

        if (!getArguments().containsKey(ARG_BOOK_ID)) {
            throw new IllegalArgumentException(BookPrefaceFragment.class.getSimpleName() + " has no book id passed");
        }

        if (!getArguments().containsKey(ARG_BOOK_PREFACE)) {
            throw new IllegalArgumentException(BookPrefaceFragment.class.getSimpleName() + " has no book preface passed");
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
                    BookPrefaceFragment.FRAGMENT_TAG,
                    BookUtils.getFragmentTitleForBook(book),
                    BookUtils.getFragmentSubtitleForBook(getContext(), book),
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

        menu.clear();

        inflater.inflate(R.menu.close_done_delete, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item);

        switch (item.getItemId()) {
            case R.id.close:
                listener.onBookPrefaceEditCancelRequest();
                return true;

            case R.id.done:
                save(contentView.getText().toString());
                return true;

            case R.id.delete:
                save("");
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void save(String preface) {
        book.setPreface(preface);

        parsePrefaceForInBufferSettings(book, preface);

        listener.onBookPrefaceEditSaveRequest(book);
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
