package com.orgzly.android.ui.views

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * Simple {@link androidx.viewpager.widget.PagerAdapter} implementation which can contain an array
 * of fragments.
 */
class ArrayPagerAdapter : FragmentPagerAdapter {
    constructor(fm: FragmentManager?, fragments: Array<Fragment>) : super(fm) {
        this.mFragments = fragments
    }

    private val mFragments: Array<Fragment>

    override fun getItem(position: Int): Fragment {
        return if (position >= mFragments.size || position < 0) {
            Log.e(TAG, "Trying to get non-existent position $position in pager with ${mFragments.size} elements, returning default")
            mFragments[0]
        } else {
            mFragments[position]
        }
    }

    override fun getCount(): Int {
        return mFragments.size
    }

    companion object {
        private val TAG = ArrayPagerAdapter::class.java.name
    }
}