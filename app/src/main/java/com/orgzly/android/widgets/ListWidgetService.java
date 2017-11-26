package com.orgzly.android.widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;
import com.orgzly.BuildConfig;
import com.orgzly.android.AppIntent;
import com.orgzly.android.util.LogUtils;

public class ListWidgetService extends RemoteViewsService {
    private static final String TAG = ListWidgetService.class.getName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "onGetViewFactory");

        return new ListWidgetViewsFactory(getApplicationContext(), intent.getStringExtra(AppIntent.EXTRA_QUERY_STRING));
    }
}
