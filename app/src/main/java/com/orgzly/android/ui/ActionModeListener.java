package com.orgzly.android.ui;

import android.support.v7.view.ActionMode;

public interface ActionModeListener {
    void updateActionModeForSelection(int selectedCount, ActionMode.Callback actionMode);

    ActionMode getActionMode();

    void actionModeDestroyed();
}
