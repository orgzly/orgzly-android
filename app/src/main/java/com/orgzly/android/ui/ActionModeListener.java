package com.orgzly.android.ui;

import androidx.fragment.app.Fragment;
import androidx.appcompat.view.ActionMode;

public interface ActionModeListener {
    void updateActionModeForSelection(int selectedCount, Fragment fragment);

    ActionMode getActionMode();

    void actionModeDestroyed();
}
