package com.orgzly.android.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.orgzly.R;
import com.orgzly.android.ui.util.ActivityUtils;

public class SyncFab {

    /**
     * Update FAB's look and action
     */
    public static void updateFab(FragmentActivity activity, String fragmentTag) {
        FloatingActionButton fab = (FloatingActionButton) activity.findViewById(R.id.fab2);

        if (fab == null) {
            return;
        }

        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(fragmentTag);

        if (fragment == null) {
            fab.hide();
            return;
        }

        if (fragment instanceof SyncFabIf) {
            final Runnable fabAction = ((SyncFabIf) fragment).getSyncFabAction();
            ActivityUtils.FragmentResources resources = new ActivityUtils.FragmentResources(activity, fragmentTag);

            if (fabAction != null) {
                fab.show();

                fab.setBackgroundTintList(ColorStateList.valueOf(resources.actionColor));
                // fab.setImageDrawable(resources.fab2Drawable);

                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fabAction.run();
                    }
                });

            } else {
                fab.hide();
            }
        } else {
            fab.hide();
        }
    }
}
