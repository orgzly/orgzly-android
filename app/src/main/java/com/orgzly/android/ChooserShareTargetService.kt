package com.orgzly.android

import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import androidx.annotation.RequiresApi
import com.orgzly.R
import com.orgzly.android.data.DataRepository
import com.orgzly.android.query.Condition
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.DottedQueryBuilder
import com.orgzly.org.OrgFileSettings
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.M)
class ChooserShareTargetService : ChooserTargetService() {
    @Inject
    lateinit var dataRepository: DataRepository

    override fun onCreate() {
        App.appComponent.inject(this)

        super.onCreate()
    }

    override fun onGetChooserTargets(componentName: ComponentName?, intentFilter: IntentFilter?): MutableList<ChooserTarget> {
        val targets = arrayListOf<ChooserTarget>()

        val icon = Icon.createWithResource(this, R.mipmap.cic_shortcut_notebook)

        for (bookView in dataRepository.getBooks()) {
            val book = bookView.book

            val direct = book.preface?.let {
                OrgFileSettings.fromPreface(it)?.getLastKeywordValue(DIRECT_SHARE)
            }

            if (direct.isNullOrBlank()) {
                continue
            }

            val score = 1.0F // Can we guess a score based on recent use or content?

            val bundle = Bundle()
            val query = DottedQueryBuilder().build(Query(Condition.InBook(book.name)))
            bundle.putString(AppIntent.EXTRA_QUERY_STRING, query)

            targets.add(ChooserTarget(book.name, icon, score, componentName, bundle))
        }

        return targets
    }

    companion object {
        private const val DIRECT_SHARE = "ORGZLY_DIRECT_SHARE"
    }
}