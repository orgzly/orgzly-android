package com.orgzly.android.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

/**
 * A [ViewPager] which supports changing height depending on the current child being viewed.
 * The adapter needs to be inherit from [androidx.fragment.app.FragmentPagerAdapter] for this
 * to work.
 *
 * @see ArrayPagerAdapter
 * @see com.orgzly.android.ui.repo.GitRepoActivity for example use
 */
class AdaptableHeightViewPager : ViewPager {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // This call instantiates the child views if they're being shown, ensuring we can get currentView
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        var adaptableHeightAdapter = adapter as FragmentPagerAdapter
        val currentView = adaptableHeightAdapter.getItem(currentItem).view
        if (currentView != null) {
            // Let the view take however much space it wants..
            currentView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            val height = Math.max(0, currentView.measuredHeight);
            val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            // .. we're now only allowed exactly that much space since the ViewPager it self doesn't take any space
            super.onMeasure(widthMeasureSpec, newHeightMeasureSpec);
        }
    }

    /**
     * Ensures that [AdaptableHeightViewPager] changes height as pages change
     */
    class TabListener(private val authPager: AdaptableHeightViewPager) : TabLayout.OnTabSelectedListener {
        override fun onTabReselected(tab: TabLayout.Tab?) {}

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.let {
                authPager.currentItem = it.position
                authPager.requestLayout()
            }
        }
    }
}
