package com.orgzly.android.ui.fragments.browser;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.util.LogUtils;

import java.util.ArrayList;

/**
 * Generic fragment for browser.
 * File browser or notes browser (for refiling) could extend it.
 */
public abstract class BrowserFragment extends ListFragment {
    private static final String TAG = BrowserFragment.class.getName();

    protected static final String ARG_ITEM = "item";

    protected BrowserFragmentListener mListener;

    private LinearLayout mShortcutsLayout;

    protected TextView mCurrentItemView;
    protected Item[] mItemList;
    protected String mCurrentItem;
    protected String mNextItem;
    protected ArrayList<String> mItemHistory = new ArrayList<>();

    protected void init(String entry) {
        Bundle args = new Bundle();

        args.putString(ARG_ITEM, entry);

        setArguments(args);
    }

    @Override
    public void onAttach(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getActivity());

        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (BrowserFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + BrowserFragmentListener.class);
        }

        final Bundle arguments = getArguments();
        /* Sets current item. Either uses passed argument, or default. */
        if (arguments != null) {
            if (arguments.containsKey(ARG_ITEM)) {
                mNextItem = arguments.getString(ARG_ITEM);
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Using passed argument: " + mNextItem);
            }
        }

        if (mNextItem == null) {
            mNextItem = defaultPath();
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Using browser's argument: " + mNextItem);
        }
    }

    abstract String defaultPath();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.browser, container, false);

        // Shortcuts
        mShortcutsLayout = (LinearLayout) view.findViewById(R.id.browser_shortcuts);
        setupShortcuts(mShortcutsLayout);

        // Current item
        mCurrentItemView = (TextView) view.findViewById(R.id.browser_title);

        // Buttons on the bottom
        setupActionButtons(view);

        return view;
    }

    abstract void setupShortcuts(LinearLayout layout);

    private void setupActionButtons(View view) {
        view.findViewById(R.id.browser_button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBrowserCancel();
            }
        });

        view.findViewById(R.id.browser_button_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBrowserCreate(mCurrentItem);
            }
        });

        view.findViewById(R.id.browser_button_use).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBrowserUse(mCurrentItem);
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState);

        super.onSaveInstanceState(outState);

        outState.putString(ARG_ITEM, mCurrentItem);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        throw new IllegalStateException("Browser implementations must implement onListItemClick");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_ITEM)) {
            mNextItem = savedInstanceState.getString(ARG_ITEM);
        }
    }

    protected class Item {
        boolean isUp = false;

        public String name;
        public int icon;

        public Item(Integer icon) {
            this.icon = icon;
            this.isUp = true;
            this.name = "Up";
        }

        public Item(Integer icon, String name) {
            this.icon = icon;
            this.isUp = false;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public interface BrowserFragmentListener {
        void onBrowserCancel();
        void onBrowserCreate(String currentItem);
        void onBrowserUse(String item);
    }
}
