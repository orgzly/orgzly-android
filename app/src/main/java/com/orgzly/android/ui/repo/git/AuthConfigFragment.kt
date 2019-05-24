package com.orgzly.android.ui.repo.git

import androidx.fragment.app.Fragment

open abstract class AuthConfigFragment : Fragment() {
    /**
     * Should validate the contents of this fragment
     *
     * @return true if the validation succeeded (i.e. the values in the fragment are valid),
     * false otherwise
     */
    abstract fun validate(): Boolean
}