package com.orgzly.android.ui.repo.git

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.orgzly.R

class HTTPSAuthConfigFragment : AuthConfigFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_git_https_config, container, false)
    }

    override fun validate(): Boolean {
        return false
    }
}
