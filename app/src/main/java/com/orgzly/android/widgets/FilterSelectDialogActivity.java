package com.orgzly.android.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.ui.fragments.FiltersFragment;
import com.orgzly.android.util.LogUtils;

public class FilterSelectDialogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {
    private static final String TAG = FilterSelectDialogActivity.class.getName();

    private SimpleCursorAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_filter_select_dialog);

        /* Create adapter using Cursor. */
        mListAdapter = FiltersFragment.createFilterCursorAdapter(this);

        ListView list = (ListView) findViewById(R.id.filter_select_list);

        list.setAdapter(mListAdapter);

        list.setEmptyView(findViewById(R.id.filter_select_no_filters));

        list.setOnItemClickListener(this);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return FiltersClient.getCursorLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mListAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mListAdapter.changeCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "onItemClick", id);

        /* notify Widget */
        Intent intent = new Intent(this, ListWidgetProvider.class);
        intent.setAction(AppIntent.ACTION_LIST_WIDGET_SET_FILTER);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
        intent.putExtra(ListWidgetProvider.EXTRA_FILTER_ID, id);
        sendBroadcast(intent);

        setResult(RESULT_OK);
        finish();
    }
}
