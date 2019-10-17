package com.orgzly.android.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ListView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ui.util.ExtensionsKt;
import com.orgzly.android.util.LogUtils;

import java.util.HashMap;

public class GesturedListView extends ListView implements GestureDetector.OnGestureListener {
    private static final String TAG = GesturedListView.class.getName();

    private int minFlingVelocity;
    private int maxFlingVelocity;
    // private int touchSlop;
    private GestureDetector gestureDetector;
    private Drawable selector;

    private GesturedListViewItemMenus itemMenus;

    private boolean scrolledHorizontally;
    private boolean scrolledVertically;
    private boolean isItemToolbarActive;
    private int itemPosition;


    public GesturedListView(Context context) {
        super(context);
        init(null);
    }

    public GesturedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    public GesturedListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GesturedListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        maxFlingVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
        minFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        // touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        gestureDetector = new GestureDetector(getContext(), this);
        selector = getSelector();

        int menuContainerId = 0;
        HashMap<Gesture, Integer> gestureMenuMap = new HashMap<>();

        /* Get attributes from XML. */
        if (attrs != null) {
            menuContainerId = ExtensionsKt.styledAttributes(getContext(), attrs, R.styleable.GesturedListView, typedArray -> {
                int id = typedArray.getResourceId(R.styleable.GesturedListView_menu_container, 0);

                int child;

                child = typedArray.getInt(R.styleable.GesturedListView_menu_for_fling_left, -1);
                if (child != -1) {
                    gestureMenuMap.put(Gesture.FLING_LEFT, child);
                }

                child = typedArray.getInt(R.styleable.GesturedListView_menu_for_fling_right, -1);
                if (child != -1) {
                    gestureMenuMap.put(Gesture.FLING_RIGHT, child);
                }

                return id;
            });
        }

        /* Disable selector. */
        // setSelector(android.R.color.transparent);

        itemMenus = new GesturedListViewItemMenus(this, gestureMenuMap, menuContainerId);
    }

    public GesturedListViewItemMenus getItemMenus() {
        return itemMenus;
    }

    public void setOnItemMenuButtonClickListener(OnItemMenuButtonClickListener listener) {
        itemMenus.setListener(listener);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        if (! isItemToolbarActive) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position, id);
            return super.performItemClick(view, position, id);

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Ignoring click as toolbar is active", position, id);
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, ev);

        // if (true) return super.onTouchEvent(e);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, friendlyMotionEvent(ev));

                /* Remember item on first touch down.
                 * Any potential gesture will be applied to this item.
                 */
                itemPosition = this.pointToPosition((int) ev.getX(), (int) ev.getY());

                /* Reset flags. */
                scrolledHorizontally = false;
                scrolledVertically = false;
                isItemToolbarActive = false;

                // itemMenus.closeAll();

                // setEnabled(true);
                setSelector(selector);

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;

            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_SCROLL:
                break;
        }

        gestureDetector.onTouchEvent(ev);

        boolean r = super.onTouchEvent(ev);

        /* Enable list only after calling super, as we don't want click feedback
         * if menu has been opened by this gesture.
         */
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, friendlyMotionEvent(ev));

            scrolledHorizontally = false;
            scrolledVertically = false;

            // setEnabled(true);
            // setSelector(selector);
        }

        return r;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, friendlyMotionEvent(ev));

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onDown(MotionEvent ev) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, friendlyMotionEvent(ev));
        return false;
    }

    @Override
    public void onShowPress(MotionEvent ev) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, friendlyMotionEvent(ev));
        // scrolledVertically = true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent ev) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, friendlyMotionEvent(ev));
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent ev1, MotionEvent ev2, float distanceX, float distanceY) {
        // if (BuildConfig.LOG_DEBUG) Dlog.method(TAG, distanceX, distanceY);

        if (Math.abs(distanceX) > Math.abs(distanceY)) {
            if (! scrolledVertically) {
                if (! scrolledHorizontally) {
                    scrolledHorizontally = true;

                    // setEnabled(false);
                    setSelector(android.R.color.transparent);

                    if (BuildConfig.LOG_DEBUG)
                        LogUtils.d(TAG, "Horizontal scroll detected and no vertical, ListView disabled");

                } else {
                    if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Horizontal scroll detected and no vertical, ListView was already disabled");
                }

            } else {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Horizontal scroll detected, but so was vertical");
            }

        } else if (Math.abs(distanceX) < Math.abs(distanceY)) {
            scrolledVertically = true;
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Vertical scroll detected");
        }

        return false;
    }

    @Override
    public void onLongPress(MotionEvent ev) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, friendlyMotionEvent(ev));
    }

    @Override
    public boolean onFling(MotionEvent ev1, MotionEvent ev2, float velocityX, float velocityY) {
        if (isItemToolbarActive) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Quick-menu already active");
            return false;
        }

        /* Only if we never scrolled vertically. */
        if (scrolledHorizontally) {
            int horizontalFling = isHorizontalFling(velocityX, velocityY);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "After scrolling horizontally: " + horizontalFling);

            if (horizontalFling != 0) {
                isItemToolbarActive = true;

                /* INVALID_POSITION can happen after swiping empty space below notes
                 * (when there are only few on the top).
                 */
                if (itemPosition != AdapterView.INVALID_POSITION) {
                    Gesture gesture = horizontalFling == 1 ?
                            Gesture.FLING_RIGHT : Gesture.FLING_LEFT;

                    boolean menuFound = itemMenus.open(itemPosition, gesture);

                    /*
                     * If it's the last item in the list scroll to it to make quick-menu visible.
                     * Wait for quick-menu opening animation to end.
                     */
                    if (menuFound && itemPosition == getCount() - 1) {
                        new Handler().postDelayed(() ->
                                        smoothScrollToPosition(itemPosition),
                                getResources().getInteger(R.integer.quick_bar_animation_duration));
                    }
                }

                return true;
            }

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "After not scrolling horizontally");
        }

        return false;
    }

    /**
     * @return -1 for left fling, 1 for right fling, 0 if the fling is not horizontal
     */
    private int isHorizontalFling(float velocityX, float velocityY) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, velocityX, velocityY, minFlingVelocity, maxFlingVelocity);

        boolean isHorizontalFLing =
                Math.abs(velocityX) > Math.abs(velocityY) && // More horizontal then vertical
                Math.abs(velocityX) >= minFlingVelocity && Math.abs(velocityX) <= maxFlingVelocity;

        if (isHorizontalFLing) {
            return velocityX > 0 ? 1 : -1;

        } else {
            return 0;
        }
    }

    private String friendlyMotionEvent(MotionEvent ev) {
        String action;

        if (ev != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    action = "ACTION_DOWN";
                    break;
                case MotionEvent.ACTION_UP:
                    action = "ACTION_UP";
                    break;
                case MotionEvent.ACTION_MOVE:
                    action = "ACTION_MOVE";
                    break;
                case MotionEvent.ACTION_CANCEL:
                    action = "ACTION_CANCEL";
                    break;
                case MotionEvent.ACTION_SCROLL:
                    action = "ACTION_SCROLL";
                    break;
                default:
                    action = String.valueOf(ev.getAction());

            }
        } else {
            action = "MotionEvent is null (action cannot be taken from it)";
        }

        return action;
    }

    public interface OnItemMenuButtonClickListener {
        void onMenuButtonClick(View itemView, int buttonId, long itemId);
    }

    public enum Gesture {
        FLING_RIGHT,
        FLING_LEFT
    }
}
