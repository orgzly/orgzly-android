package com.orgzly.android.ui.drawer

/**
 * Implemented by fragments opened using the drawer.
 *
 * Used to flag (check) [android.view.MenuItem] active fragment in the drawer.
 */
interface DrawerItem {
    fun getCurrentDrawerItemId(): String
}