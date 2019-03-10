package com.orgzly.android.ui.notes.quickbar

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

class ItemGestureDetector(context: Context, private val listener: Listener) :
        RecyclerView.OnItemTouchListener {

    private val viewConfiguration = ViewConfiguration.get(context)

    private val maxFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity
    private val minFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity

    private val gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, e1.action, e2.action)

            val isFling = isHorizontalFling(velocityX, velocityY)

            return if (isFling != 0) {
                listener.onFling(isFling, e1.x, e1.y)
                true
            } else {
                false
            }
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, e1.action, e2.action)
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    })

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, e.action)
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, e.action)
        return gestureDetector.onTouchEvent(e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }


    /**
     * @return -1 for left fling, 1 for right fling, 0 if the fling is not horizontal
     */
    private fun isHorizontalFling(velocityX: Float, velocityY: Float): Int {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, velocityX, velocityY, minFlingVelocity, maxFlingVelocity)

        val isHorizontalFLing =
                Math.abs(velocityX) > Math.abs(velocityY) /* More horizontal then vertical*/ &&
                Math.abs(velocityX) >= minFlingVelocity && Math.abs(velocityX) <= maxFlingVelocity

        return if (isHorizontalFLing) {
            if (velocityX > 0) 1 else -1
        } else {
            0
        }
    }

    interface Listener {
        fun onFling(direction: Int, x: Float, y: Float)
    }

    companion object {
        private val TAG = ItemGestureDetector::class.java.name
    }
}
