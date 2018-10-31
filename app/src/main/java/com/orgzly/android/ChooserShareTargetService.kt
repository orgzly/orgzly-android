package com.orgzly.android

import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import android.support.annotation.RequiresApi
import com.orgzly.R
import com.orgzly.android.query.Condition
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.DottedQueryBuilder

@RequiresApi(Build.VERSION_CODES.M)
class ChooserShareTargetService : ChooserTargetService() {
    override fun onGetChooserTargets(componentName: ComponentName?, intentFilter: IntentFilter?): MutableList<ChooserTarget> {
        val shelf = Shelf(this)

        val targets = arrayListOf<ChooserTarget>()

        val icon = Icon.createWithResource(this, R.drawable.ic_logo_for_widget)
        for (book in shelf.books) {
            val direct = book.orgFileSettings.getLastKeywordValue(DIRECT_SHARE)
            if (direct.isNullOrBlank()) {
                continue
            }
            val score = 1.0F // Can we guess a score based on recent use or content?
            val query = DottedQueryBuilder().build(Query(Condition.InBook(book.name)))
            val bundle = Bundle()
            bundle.putString(AppIntent.EXTRA_FILTER, query)
            targets.add(ChooserTarget(book.name, icon, score, componentName, bundle))
        }
        return targets
    }

    companion object {
        private const val DIRECT_SHARE = "ORGZLY_DIRECT_SHARE"
    }
}