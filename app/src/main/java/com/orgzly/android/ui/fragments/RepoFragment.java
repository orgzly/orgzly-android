package com.orgzly.android.ui.fragments;

import android.support.v4.app.Fragment;

import com.orgzly.android.repos.Repo;

public class RepoFragment extends Fragment {
    public interface RepoFragmentListener {
        void onRepoCreateRequest(Repo repo);
        void onRepoUpdateRequest(long id, Repo repo);
        void onRepoCancelRequest();
    }
}
