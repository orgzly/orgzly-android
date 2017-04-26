package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StyleableRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class DrawerFragment extends ListFragment
        implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = DrawerFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = DrawerFragment.class.getName();

    /* Drawer list items. */
    private FiltersItem filtersHeader;
    private final List<FilterItem> filters = new ArrayList<>();
    private BooksItem booksHeader;
    private final List<BookItem> books = new ArrayList<>();
    private SettingsItem settingsHeader;
    private AgendaItem agendaHeader;

    private DrawerFragmentListener mListener;
    private ArrayAdapter<DrawerItem> mListAdapter;
    private String activeFragmentTag = null;
    private DrawerItem selectedItem = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DrawerFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }

    public static DrawerFragment getInstance() {
        return new DrawerFragment();
    }

    public void setActiveFragment(String fragmentTag) {
        activeFragmentTag = fragmentTag;

        updateSelectedItemFromActiveFragment();
    }

    private void updateSelectedItemFromActiveFragment() {
        selectedItem = null;

        FragmentActivity activity = getActivity();
        if (activity != null) {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(activeFragmentTag);

            if (fragment != null) {
                /* Find by query string. */
                if (QueryFragment.FRAGMENT_TAG.equals(activeFragmentTag)) {

                    SearchQuery query = ((QueryFragment) fragment).getQuery();

                    if (query != null) {
                        for (FilterItem item : filters) {
                            if (query.toString().equals(item.query)) {
                                selectedItem = item;
                            }
                        }
                    }

                } else if (BooksFragment.FRAGMENT_TAG.equals(activeFragmentTag)) {
                    selectedItem = booksHeader;

                } else if (FiltersFragment.FRAGMENT_TAG.equals(activeFragmentTag)) {
                    selectedItem = filtersHeader;

                } else if (SettingsFragment.FRAGMENT_TAG.equals(activeFragmentTag)) {
                    selectedItem = settingsHeader;

                /* Find by book ID. */
                } else if (BookFragment.FRAGMENT_TAG.equals(activeFragmentTag)) {
                    Book book = ((BookFragment) fragment).getBook();

                    if (book != null) {
                        for (BookItem item : books) {
                            if (book.getId() == item.id) {
                                selectedItem = item;
                            }
                        }
                    }
                } else if (AgendaFragment.FRAGMENT_TAG.equals(activeFragmentTag)) {
                    selectedItem = agendaHeader;
                }

                mListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getActivity());
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (DrawerFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + DrawerFragmentListener.class);
        }

        /* Setup drawer list's header items. */
        filtersHeader = new FiltersItem();
        booksHeader = new BooksItem();
        settingsHeader = new SettingsItem();
        agendaHeader = new AgendaItem();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        /* Inflate layout. */
        return inflater.inflate(R.layout.fragment_left_drawer, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        mListAdapter = createAdapter();
        setListAdapter(mListAdapter);

        if (savedInstanceState != null) { // Configuration change.
            getActivity().getSupportLoaderManager().initLoader(Loaders.DRAWER_BOOKS, null, this);
            getActivity().getSupportLoaderManager().initLoader(Loaders.DRAWER_FILTERS, null, this);
        } else {
            getActivity().getSupportLoaderManager().restartLoader(Loaders.DRAWER_BOOKS, null, this);
            getActivity().getSupportLoaderManager().restartLoader(Loaders.DRAWER_FILTERS, null, this);
        }
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroyView();

        setListAdapter(null);
        mListAdapter = null;
    }

    @Override
    public void onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        /* In case books sort order preference has been changed. */
        getActivity().getSupportLoaderManager().restartLoader(Loaders.DRAWER_BOOKS, null, this);

         /* Start to listen for any preference changes. */
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        /* Stop listening for preference changed. */
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
    }

    private ArrayAdapter<DrawerItem> createAdapter() {
        return new ArrayAdapter<DrawerItem>(getActivity(), R.layout.item_drawer, R.id.item_drawer_text) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ViewHolder holder = (ViewHolder) view.getTag();

                if (holder == null) {
                    holder = new ViewHolder();
                    holder.container = (ViewGroup) view.findViewById(R.id.item_drawer_container);
                    holder.text = (TextView) view.findViewById(R.id.item_drawer_text);
                    holder.leftIcon = (ImageView) view.findViewById(R.id.item_drawer_left_icon);
                    holder.rightIcon = (ImageView) view.findViewById(R.id.item_drawer_right_icon);
                    holder.activeFlag = view.findViewById(R.id.item_drawer_active_flag);
                    view.setTag(holder);
                }

                DrawerItem item = getItem(position);

                // item.textSize, item.leftIconResource});
                TypedArray iconAttrs = getContext().obtainStyledAttributes(R.styleable.Icons);
                TypedArray fontSizeAttrs = getContext().obtainStyledAttributes(R.styleable.FontSize);

                /* Set text size. */
                int t = fontSizeAttrs.getDimensionPixelSize(item.textSize, -1);
                if (t != -1) {
                    holder.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, t);
                }


                /* Set or hide left icon. */
                if (item.icon != 0) {
                    holder.leftIcon.setImageResource(iconAttrs.getResourceId(item.icon, -1));
                    holder.leftIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.leftIcon.setVisibility(View.INVISIBLE);
                }

                iconAttrs.recycle();
                fontSizeAttrs.recycle();


                /* Set or remove right icon. */
                if (item.isModified) {
                    holder.rightIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.rightIcon.setVisibility(View.INVISIBLE);
                }

                /* Set text typeface. */
                holder.text.setTypeface(null, item.typeface);

                /* Set alpha. */
                view.setAlpha(item.alpha);

                /* Mark currently displayed fragment. */
                if (item == selectedItem) {
                    holder.activeFlag.setVisibility(View.VISIBLE);
                } else {
                    holder.activeFlag.setVisibility(View.INVISIBLE);
                }

                return view;
            }
        };
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id, bundle);

        switch (id) {
            case Loaders.DRAWER_FILTERS:
                return FiltersClient.getCursorLoader(getActivity());

            case Loaders.DRAWER_BOOKS:
                return BooksClient.getCursorLoader(getActivity());

            default:
                throw new IllegalArgumentException("Loader id " + id + " unexpected");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader, cursor);

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        switch (loader.getId()) {
            case Loaders.DRAWER_FILTERS:
                updateFromFiltersCursor(cursor);
                updateAdapter();
                break;

            case Loaders.DRAWER_BOOKS:
                updateFromBooksCursor(cursor);
                updateAdapter();
                break;
        }
    }

    private void updateAdapter() {
        updateSelectedItemFromActiveFragment();

        mListAdapter.clear();

        mListAdapter.add(filtersHeader);

        for (DrawerItem item: filters) {
            mListAdapter.add(item);
        }

        mListAdapter.add(agendaHeader);

        mListAdapter.add(booksHeader);

        for (DrawerItem item: books) {
            mListAdapter.add(item);
        }

        mListAdapter.add(settingsHeader);
    }

    private void updateFromFiltersCursor(Cursor cursor) {
        filters.clear();

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ProviderContract.Filters.Param.NAME));
            String query = cursor.getString(cursor.getColumnIndex(ProviderContract.Filters.Param.QUERY));

            FilterItem item = new FilterItem(name, new SearchQuery(query).toString());

            filters.add(item);
        }
    }

    private void updateFromBooksCursor(Cursor cursor) {
        books.clear();

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ProviderContract.Books.Param.NAME));
            long id = cursor.getLong(cursor.getColumnIndex(ProviderContract.Books.Param._ID));

            Book book = BooksClient.fromCursor(cursor);

            /* Set book name from title if it exists. */
            if (book.getOrgFileSettings().getTitle() != null) {
                name = book.getOrgFileSettings().getTitle();
            }

            BookItem item = new BookItem(name, id);

            item.isModified = book.isModifiedAfterLastSync();

            /* Change opacity for dummy notebook. */
            if (book.isDummy()) {
                item.alpha = 0.4f;
            }

            books.add(item);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mListener != null) {
            DrawerItem item = (DrawerItem) l.getItemAtPosition(position);

            mListener.onDrawerItemClicked(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        /* Restart loader if notebooks sort order changed. */
        FragmentActivity activity = getActivity();
        if (activity != null && getString(R.string.pref_key_notebooks_sort_order).equals(key)) {
            activity.getSupportLoaderManager().restartLoader(Loaders.DRAWER_BOOKS, null, this);
        }
    }

    public interface DrawerFragmentListener {
        void onDrawerItemClicked(DrawerItem item);
    }


    private class ViewHolder {
        ViewGroup container;
        TextView text;
        ImageView leftIcon;
        ImageView rightIcon;
        View activeFlag;
    }

    public class DrawerItem {
        String name;

        float alpha = 1;

        boolean isModified = false;

        @StyleableRes int icon = 0; // No icon by default

        int typeface = Typeface.NORMAL;
        @StyleableRes int textSize = R.styleable.FontSize_item_drawer_text_size;

        public String toString() {
            return name;
        }
    }

    public class FiltersItem extends DrawerItem {
        FiltersItem() {
            this.name = getString(R.string.searches);
            this.icon = R.styleable.Icons_oic_drawer_filters;
            this.textSize = R.styleable.FontSize_item_drawer_title_text_size;
        }
    }

    public class FilterItem extends DrawerItem {
        public String query;

        FilterItem(String name, String query) {
            this.name = name;
            this.query = query;
        }
    }

    public class BooksItem extends DrawerItem {
        BooksItem() {
            this.name = getString(R.string.notebooks);
            this.icon = R.styleable.Icons_oic_drawer_notebooks;
            this.textSize = R.styleable.FontSize_item_drawer_title_text_size;
        }
    }

    public class BookItem extends DrawerItem {
        public long id;

        BookItem(String name, long id) {
            this.name = name;
            this.id = id;
        }
    }

    public class SettingsItem extends DrawerItem {
        SettingsItem() {
            this.name = getString(R.string.settings);
            this.icon = R.styleable.Icons_oic_drawer_settings;
            this.textSize = R.styleable.FontSize_item_drawer_title_text_size;
        }
    }

    public class AgendaItem extends DrawerItem {
        AgendaItem() {
            this.name = getString(R.string.agenda);
            this.icon = R.styleable.Icons_oic_drawer_agenda;
            this.textSize = R.styleable.FontSize_item_drawer_title_text_size;
        }
    }
}
