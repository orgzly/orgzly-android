package com.orgzly.android.ui.repo.git

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.orgzly.R

class SSHAuthConfigFragment : AuthConfigFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_git_ssh_key_config, container, false)
    }

    override fun validate(): Boolean {
        return false
    }
}