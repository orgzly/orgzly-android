package com.orgzly.android.util;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.orgzly.R;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.util.ActivityUtils;

public class AppPermissions {
    public static final int FOR_LOCAL_REPO = 1;
    public static final int FOR_BOOK_EXPORT = 2;
    public static final int FOR_SYNC_START = 3;

    public static boolean isNotGranted(Context context, int requestCode) {
        String permission = permissionForRequest(requestCode);

        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGrantedOrRequest(final CommonActivity activity, int requestCode) {
        String permission = permissionForRequest(requestCode);
        int rationale = rationaleForRequest(requestCode);

        if (isNotGranted(activity, requestCode)) {
            /* Should we show an explanation? */
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                View view = activity.findViewById(R.id.main_content);

                activity.showSnackbar(Snackbar.make(view, rationale, MiscUtils.SNACKBAR_WITH_ACTION_DURATION)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityUtils.openAppInfoSettings(activity);
                            }
                        }));

            } else {
                /* No explanation needed -- request the permission. */
                ActivityCompat.requestPermissions(activity, new String[]{ permission }, requestCode);
            }

            return false;

        } else {
            return true;
        }
    }


    /**
     * Mapping of request code to actual permissions.
     */
    private static String permissionForRequest(int requestCode) {
        switch (requestCode) {
            case FOR_LOCAL_REPO:
                return Manifest.permission.WRITE_EXTERNAL_STORAGE;
            case FOR_BOOK_EXPORT:
                return Manifest.permission.WRITE_EXTERNAL_STORAGE;
            case FOR_SYNC_START:
                return Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }

        throw new IllegalArgumentException("Unknown permission for request code " + requestCode);
    }

    /**
     * Mapping of request code to explanation.
     */
    private static int rationaleForRequest(int requestCode) {
        switch (requestCode) {
            case FOR_LOCAL_REPO:
                return R.string.permissions_rationale_for_local_repo;
            case FOR_BOOK_EXPORT:
                return R.string.permissions_rationale_for_book_export;
            case FOR_SYNC_START:
                return R.string.permissions_rationale_for_sync_start;
        }

        throw new IllegalArgumentException("Unknown rationale for request code " + requestCode);
    }
}
