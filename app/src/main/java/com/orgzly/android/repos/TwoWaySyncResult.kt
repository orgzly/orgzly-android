package com.orgzly.android.repos

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.File

/**
 * Result of doing a two-way synchronization.
 * @param newRook The new versioned book that was created by this synchronization operation
 * @param loadFile This file contains the new data that should be loaded to the local notebook that
 * was just synchronized. When it is null there's no new information to load, meaning that the
 * current contents of the notebook are up to date.
 * @param onMainBranch Boolean indicating whether the sync resulted in a commit on the main branch,
 * or on a temporary branch.
 * @see GitRepo Which implements [TwoWaySyncRepo]
 * @see TwoWaySyncRepo
 */
data class TwoWaySyncResult(val newRook: @NotNull VersionedRook, val onMainBranch: Boolean, val loadFile: @Nullable File?)