package com.orgzly.android.ui.views;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.android.ui.util.ListViewUtils;
import com.orgzly.android.ui.util.ViewUtils;
import com.orgzly.android.util.LogUtils;

import java.util.HashMap;
import java.util.List;

public class GesturedListViewItemMenus {
    private static final String TAG = GesturedListViewItemMenus.class.getName();

    private final GesturedListView gesturedListView;
    private final HashMap<GesturedListView.Gesture, Integer> gestureMenuMap;
    private final int menuContainerId;

    private final LongSparseArray<GesturedListViewItemMenu> openedMenus = new LongSparseArray<>();

    private GesturedListView.OnItemMenuButtonClickListener mListener;


    public GesturedListViewItemMenus(GesturedListView gesturedListView, HashMap<GesturedListView.Gesture, Integer> gestureMenuMap, int containerId) {
        this.gesturedListView = gesturedListView;
        this.gestureMenuMap = gestureMenuMap;
        this.menuContainerId = containerId;
    }

    /**
     *
     * @param itemPosition
     * @param gesture
     * @return true if menu exists, false if not
     */
    public boolean open(int itemPosition, GesturedListView.Gesture gesture) {
        long itemId = gesturedListView.getItemIdAtPosition(itemPosition);

        /* Close all except this one. */
        closeAllExcept(itemId);

        View itemView = ListViewUtils.getViewIfVisible(gesturedListView, itemPosition);

        if (itemView == null) {
            return false;
        }

        ViewGroup menuContainer = getMenuContainer(itemView);

        /* Find ViewFlipper within container. */
        ViewFlipper menuFlipper = null;
        if (menuContainer != null) {
            for (int i = 0; i <= menuContainer.getChildCount(); i++) {
                View view = menuContainer.getChildAt(i);
                if (view instanceof ViewFlipper) {
                    menuFlipper = (ViewFlipper) view;
                    break;
                }
            }
        }

        if (menuFlipper != null) {
            Integer flipperChild = gestureMenuMap.get(gesture);

            if (flipperChild != null) {
                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, "GESTURE " + gesture + " @ position " + itemPosition + ": opening menu for " + itemId);

                GesturedListViewItemMenu menu = openedMenus.get(itemId);
                if (menu == null) {
                    menu = new GesturedListViewItemMenu(itemId, gesturedListView, menuContainer, menuFlipper, gestureMenuMap);
                    openedMenus.put(itemId, menu);

                } else {
                    /* If the menu for this gesture is already opened, close it. */
                    if (menu.isOpenedForGesture(gesture)) {
                        menu.startClosing(true);
                        return true;
                    }
                }

                menu.open(gesture);

                // itemView.setBackgroundResource(R.color.item_head_menu_opened);
                setItemMenuButtonOnClickListeners(itemView, itemId);

                return true;
            }

        } else {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Menu flipper not found for item at position " + itemPosition);
        }

        return false;
    }

    /**
     * Finds all buttons inside {@link #menuContainerId} and sets a click listener for each one.
     *
     * @param itemView
     * @param itemId
     */
    private void setItemMenuButtonOnClickListeners(View itemView, final long itemId) {
        ViewGroup menuContainer = getMenuContainer(itemView);

        if (menuContainer != null) {
            List<View> buttons = ViewUtils.getAllChildren(menuContainer, ImageButton.class);

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Found " + buttons.size() + " quick-menu buttons for id " + itemId);

            for (final View button : buttons) {
                button.setOnClickListener(v -> {
                    if (mListener != null) {
                        mListener.onMenuButtonClick(itemView, button.getId(), itemId);
                    }
                });
            }
        }
    }

    /**
     * Finds and returns item menu's container view.
     *
     * It can return <code>null</code> if item doesn't contain a menu (for example header view).
     *
     * @param itemView Item's View
     * @return Menu's container view or <code>null</code> if item doesn't contain a menu.
     */
    @Nullable
    private ViewGroup getMenuContainer(View itemView) {
        ViewGroup view = (ViewGroup) itemView.findViewById(menuContainerId);

        if (view == null) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Menu container for " + itemView + " not found");
        }

        return view;
    }

    public void closeAllExcept(long idToKeepOpened) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, idToKeepOpened);

        for (int i = 0; i < openedMenus.size(); i++) {
            long id = openedMenus.keyAt(i);

            if (id != idToKeepOpened) { /* Close unless it's a specified id. */
                GesturedListViewItemMenu menu = openedMenus.get(id);

                menu.startClosing(true);
            }
        }
    }

    public void closeAll() {
        for (int i = 0; i < openedMenus.size(); i++) {
            long id = openedMenus.keyAt(i);
            GesturedListViewItemMenu menu = openedMenus.get(id);
            menu.startClosing(false);
        }
    }

    /**
     * Called when binding views from the adapter.
     */
    public void updateView(View view, long noteId, ViewGroup menuContainer, ViewFlipper menuFlipper) {
        GesturedListViewItemMenu menu = openedMenus.get(noteId);

        if (menu == null) {
            menuContainer.setVisibility(View.GONE);

        } else {
            if (menu.isClosed()) {
                openedMenus.remove(noteId);

            } else {
                menu.updateView(menuContainer, menuFlipper);

                // view.setBackgroundResource(R.color.item_head_menu_opened);
                setItemMenuButtonOnClickListeners(view, noteId);
            }
        }
    }

    public void setListener(GesturedListView.OnItemMenuButtonClickListener listener) {
        mListener = listener;
    }

    public Long getOpenedId() {
        for (int i = 0; i < openedMenus.size(); i++) {
            long id = openedMenus.keyAt(i);

            GesturedListViewItemMenu menu = openedMenus.get(id);

            if (!menu.isClosed()) {
                return id;
            }
        }

        return null;
    }
}
