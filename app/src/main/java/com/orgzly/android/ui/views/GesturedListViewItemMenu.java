package com.orgzly.android.ui.views;

import androidx.core.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ui.util.ListViewUtils;
import com.orgzly.android.util.LogUtils;

import java.util.HashMap;

class GesturedListViewItemMenu {
    private static final String TAG = GesturedListViewItemMenu.class.getName();

    private static final Interpolator MENU_OPEN_INTERPOLATOR = new DecelerateInterpolator();
    private static final Interpolator MENU_CLOSE_INTERPOLATOR = new DecelerateInterpolator();

    private static final int FLING_RIGHT_OUT_ANIMATION = R.anim.slide_out_to_right;
    private static final int FLING_RIGHT_IN_ANIMATION = R.anim.slide_in_from_left;
    private static final int FLING_LEFT_OUT_ANIMATION = R.anim.slide_out_to_left;
    private static final int FLING_LEFT_IN_ANIMATION = R.anim.slide_in_from_right;

//    private static final int FLING_RIGHT_OUT_ANIMATION = R.anim.fade_out;
//    private static final int FLING_RIGHT_IN_ANIMATION = R.anim.fade_in;
//    private static final int FLING_LEFT_OUT_ANIMATION = R.anim.fade_out;
//    private static final int FLING_LEFT_IN_ANIMATION = R.anim.fade_in;

    private final int animationDuration;
    private final float containerHeight;

    private final long itemId;
    private final GesturedListView gesturedListView;

    private ViewGroup containerView;
    private ViewFlipper flipperView;
    private HashMap<GesturedListView.Gesture, Integer> gestureMenuMap;

    private GesturedListViewItemMenuState containerState;
    private int flipperDisplayedChild;

    public GesturedListViewItemMenu(long itemId, GesturedListView listView, ViewGroup container, ViewFlipper flipper, HashMap<GesturedListView.Gesture, Integer> gestureMenuMap) {
        this.animationDuration = listView.getResources().getInteger(R.integer.quick_bar_animation_duration);
        this.containerHeight = listView.getResources().getDimension(R.dimen.quick_bar_height);

        this.itemId = itemId;
        this.gesturedListView = listView;

        this.containerView = container;
        this.flipperView = flipper;
        this.gestureMenuMap = gestureMenuMap;
    }

    public void open(GesturedListView.Gesture gesture) {
        setFlipperAnimation(gesture);

        setFlipperDisplayedChild(gesture);

        if (isContainerViewGone()) {
            startOpening();
        }
    }

    /**
     * Menu that should be opened for this gesture is already opened.
     */
    public boolean isOpenedForGesture(GesturedListView.Gesture gesture) {
        if (containerView.getVisibility() == View.GONE) {
            return false;
        }

        Integer targetChild = gestureMenuMap.get(gesture);

        return targetChild != null && targetChild == flipperView.getDisplayedChild();

    }

    private void setFlipperAnimation(GesturedListView.Gesture gesture) {
        /*
         * If container is closed, we don't want flipper animation.
         * Appearance of the container itself will be animated.
         */
        if (isContainerViewGone()) {
            flipperView.setInAnimation(null);
            flipperView.setOutAnimation(null);

        } else {
            if (gesture == GesturedListView.Gesture.FLING_RIGHT) {
                flipperView.setOutAnimation(gesturedListView.getContext(), FLING_RIGHT_OUT_ANIMATION);
                flipperView.setInAnimation(gesturedListView.getContext(), FLING_RIGHT_IN_ANIMATION);

            } else if (gesture == GesturedListView.Gesture.FLING_LEFT) {
                flipperView.setOutAnimation(gesturedListView.getContext(), FLING_LEFT_OUT_ANIMATION);
                flipperView.setInAnimation(gesturedListView.getContext(), FLING_LEFT_IN_ANIMATION);

            } else {
                flipperView.setInAnimation(null);
                flipperView.setOutAnimation(null);
            }
        }
    }

    private void setFlipperDisplayedChild(GesturedListView.Gesture gesture) {
        flipperDisplayedChild = gestureMenuMap.get(gesture);

        flipIfNeeded(flipperDisplayedChild);
    }

    private void flipIfNeeded(int child) {
        if (flipperView.getDisplayedChild() != child) {
            flipperView.setDisplayedChild(child);
        }
    }

    public boolean isContainerViewGone() {
        return containerView.getVisibility() == View.GONE && containerView.getAnimation() == null;
    }

    public boolean isClosed() {
        return containerState == GesturedListViewItemMenuState.CLOSED;
    }

    private void startOpening() {
        containerState = GesturedListViewItemMenuState.OPENING;
        ViewCompat.setHasTransientState(containerView, true);

        Animation animation;
        AnimationSet animations = new AnimationSet(false);

        animations.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                containerState = GesturedListViewItemMenuState.OPENED;
                ViewCompat.setHasTransientState(containerView, false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        /* Fade in animation. */
        animation = AnimationUtils.loadAnimation(gesturedListView.getContext(), R.anim.fade_in);
        animation.setInterpolator(MENU_OPEN_INTERPOLATOR);
        animation.setDuration(animationDuration);

        animations.addAnimation(animation);

        /* Height change animation. */

        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) containerView.getLayoutParams();

        /* Initial state. */
        params.height = 0;
        containerView.setVisibility(View.VISIBLE);

        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);

                /* New margin. */
                // params.bottomMargin = - params.height + (int) ((params.height) * interpolatedTime);
                params.height = (int) (containerHeight * interpolatedTime);

                /* Invalidating the layout, to see the made changes. */
                containerView.requestLayout();

                /*
                 * Make sure entire item is visible while opening the menu.
                 * Does not work well when flinging the list,
                 * as the item keeps getting scrolled to.
                 */
                // gesturedListView.smoothScrollToPosition(itemPosition);
            }
        };
        animation.setInterpolator(MENU_OPEN_INTERPOLATOR);
        animation.setDuration(animationDuration);

        animations.addAnimation(animation);

        containerView.startAnimation(animations);
    }

    public void startClosing(boolean animate) {
        /* Already closed or closing. */
        if (containerState == GesturedListViewItemMenuState.CLOSING || containerState == GesturedListViewItemMenuState.CLOSED) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Menu already in CLOSED or CLOSING state");
            return;
        }

        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) containerView.getLayoutParams();

        if (!animate) {
            closeContainer(params);
            return;
        }

        containerState = GesturedListViewItemMenuState.CLOSING;
        ViewCompat.setHasTransientState(containerView, true);


        Animation animation;
        AnimationSet animations = new AnimationSet(false);

        animations.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                closeContainer(params);
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Menu closed - animation ended");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        /* Fade out animation. */
        animation = AnimationUtils.loadAnimation(gesturedListView.getContext(), R.anim.fade_out);
        animation.setInterpolator(MENU_CLOSE_INTERPOLATOR);
        animation.setDuration(animationDuration);

        animations.addAnimation(animation);

        /* Margin change animation. */

        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);

                /* New margin. */
                // params.bottomMargin = - (int) ((params.height) * interpolatedTime);
                params.height = (int) (containerHeight - containerHeight * interpolatedTime);

                /* Invalidating the layout, to see the made changes. */
                containerView.requestLayout();

                /* Make sure entire item is visible while opening the menu. */
                // ListView.this.smoothScrollToPosition(itemPosition);

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Closing menu (" + params.height + ")");
            }
        };
        animation.setInterpolator(MENU_CLOSE_INTERPOLATOR);
        animation.setDuration(animationDuration);

        animations.addAnimation(animation);

        if (ListViewUtils.isIdVisible(gesturedListView, itemId)) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Menu container visible, starting close animation");
            containerView.startAnimation(animations);

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Menu container NOT visible, closing immediately");
            closeContainer(params);
        }
    }

    private void closeContainer(ViewGroup.MarginLayoutParams params) {
        containerState = GesturedListViewItemMenuState.CLOSED;

        containerView.setVisibility(View.GONE);
        params.height = 0;

        containerView.requestLayout();

        ViewCompat.setHasTransientState(containerView, false);
    }

    public void updateView(ViewGroup menuContainer, ViewFlipper menuFlipper) {
        /* Update current menu views for the item. */
        containerView = menuContainer;
        flipperView = menuFlipper;

        /* Update container. */
        switch (containerState) {
            case OPENED:
                    /* Update flipper. */
//                    menu.flipperView.setInAnimation(null);
//                    menu.flipperView.setOutAnimation(null);
                flipIfNeeded(flipperDisplayedChild);
                containerView.setVisibility(View.VISIBLE);
                break;

            case OPENING:
                break;

            case CLOSED:
                containerView.setVisibility(View.GONE);
                break;

            case CLOSING:
                break;
        }
    }


    private enum GesturedListViewItemMenuState {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING
    }
}
