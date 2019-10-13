package com.orgzly.android.repos

import com.orgzly.android.db.entity.Repo

data class RepoWithProps @JvmOverloads constructor(
        val repo: Repo,
        val props: Map<String, String> = emptyMap())