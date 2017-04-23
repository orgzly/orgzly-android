package com.orgzly.android.widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;
import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;

public class ListWidgetService extends RemoteViewsService {
    private static final String TAG = ListWidgetService.class.getName();

    public static final String EXTRA_QUERY_STRING = "query_string";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "onGetViewFactory");

        return new ListWidgetViewsFactory(getApplicationContext(), intent.getStringExtra(EXTRA_QUERY_STRING));
    }
}
