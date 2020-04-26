package com.orgzly.android.ui.stickyheaders;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Taken from: https://github.com/Doist/RecyclerViewExtensions
 *
 * Adds sticky headers capabilities to the {@link RecyclerView.Adapter}. Should return {@code true} for all
 * positions that represent sticky headers.
 */
public interface StickyHeaders {
    boolean isStickyHeader(int position);

    interface ViewSetup {
        /**
         * Adjusts any necessary properties of the {@code holder} that is being used as a sticky header.
         *
         * {@link #teardownStickyHeaderView(View)} will be called sometime after this method
         * and before any other calls to this method go through.
         */
        void setupStickyHeaderView(View stickyHeader);

        /**
         * Reverts any properties changed in {@link #setupStickyHeaderView(View)}.
         *
         * Called after {@link #setupStickyHeaderView(View)}.
         */
        void teardownStickyHeaderView(View stickyHeader);
    }
}
