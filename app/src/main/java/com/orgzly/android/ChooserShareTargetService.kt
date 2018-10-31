package com.orgzly.android

import android.app.PendingIntent
import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import android.support.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_bookchooser.view.*
import android.graphics.BitmapFactory
import android.os.Bundle
import com.orgzly.R
import com.orgzly.android.query.Condition
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.DottedQueryBuilder

private const val DIRECT_SHARE = "ORGZLY_DIRECT_SHARE";

@RequiresApi(Build.VERSION_CODES.M)
class ChooserShareTargetService : ChooserTargetService() {
    private val shelf = Shelf(this)

    override fun onGetChooserTargets(componentName: ComponentName?, intentFilter: IntentFilter?): MutableList<ChooserTarget> {
        val targets = arrayListOf<ChooserTarget>();

        val icon = Icon.createWithResource(this, R.drawable.ic_logo_for_widget);
        for (book in shelf.books) {
            val direct = book.orgFileSettings.getLastKeywordValue(DIRECT_SHARE);
            if (direct.isNullOrBlank())
                continue;
            val bundle : Bundle = Bundle();
            val score : Float = 1.0F; // Can we guess a score based on recent use or content?
            val query = DottedQueryBuilder().build(Query(Condition.InBook(book.name)));
            bundle.putString(AppIntent.EXTRA_FILTER, query)
            targets.add(ChooserTarget(book.name, icon, score, componentName, bundle ));
        }
        return targets;
    }

}