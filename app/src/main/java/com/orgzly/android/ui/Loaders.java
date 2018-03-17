package com.orgzly.android.ui;

import android.support.v4.app.LoaderManager;
import android.util.SparseArray;

import java.util.HashMap;

public class Loaders {
    public static final int BOOK_FRAGMENT_BOOK = 0;
    public static final int BOOK_FRAGMENT_NOTES = 1;
    public static final int BOOKS_FRAGMENT = 2;
    public static final int QUERY_FRAGMENT = 3;
    public static final int REPOS_FRAGMENT = 4;
    public static final int FILTERS_FRAGMENT = 5;

    public static final int DRAWER_FILTERS = 6;
    public static final int DRAWER_BOOKS = 7;

    public static final int AGENDA_FRAGMENT = 8;

    private static final int NEXT_AVAILABLE_ID = 9;

    private static int nextAvailableId = NEXT_AVAILABLE_ID;

    private static SparseArray<HashMap<String, Integer>> ids = new SparseArray<>(2);

    public static int generateLoaderId(int fragmentLoaderId, String arg) {
        Integer id;

        HashMap<String, Integer> map = ids.get(fragmentLoaderId);
        if (map == null) {
            map = new HashMap<>();
            ids.put(fragmentLoaderId, map);
        }

        id = map.get(arg);
        if (id == null) {
            map.put(arg, nextAvailableId);
            id = nextAvailableId++;
        }

        return id;
    }

    /**
     * Destroy all loaders. TODO: Only when Settings change could have affected the results
     */
    public static void destroyAll(LoaderManager loaderManager) {
        for (int i = 0; i < nextAvailableId; i++) {
            if (loaderManager.getLoader(i) != null) {
                loaderManager.destroyLoader(i);
            }
        }
        nextAvailableId = NEXT_AVAILABLE_ID;
        ids.clear();
    }
}
