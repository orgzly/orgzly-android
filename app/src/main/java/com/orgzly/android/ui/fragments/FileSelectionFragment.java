package com.orgzly.android.ui.fragments;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.orgzly.R;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;

public class FileSelectionFragment extends Fragment
        implements FileBrowserOpener.BrowserResultHandler, View.OnClickListener {

    private EditText editText;
    private FileBrowserOpener browserOpener;

    public FileSelectionFragment() {}

    public void setHint(int hint) {
        editText.setHint(hint);
    }

    public void setValue(String value) {
        editText.setText(value);
    }

    public String getValue() {
        return editText.getText().toString();
    }

    public EditText getEditText() { return editText; }

    @Override
    public void handleBrowseResult(Uri uri) {
        editText.setText(uri.toString());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_file_selection, container, false);
        editText = (EditText) view.findViewById(R.id.fragment_file_selection_text);

        view.findViewById(R.id.fragment_file_selection_browse_button).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        startBrowser();
    }

    private void startBrowser() {
        /* Close the keyboard before opening the browser. */
        if (getActivity() != null) {
            ActivityUtils.closeSoftKeyboard(getActivity());
        }

        /* Do not open the browser unless we have the storage permission. */
        if (!AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(),
                AppPermissions.FOR_LOCAL_REPO)) {
            return;
        }

        String uriString = null;
        Uri uri = null;
        if (! TextUtils.isEmpty(editText.getText())) {
            uriString = editText.getText().toString();
        }

        if (uriString != null) {
            uri = Uri.parse(uriString);
        }
        browserOpener.browseDirectory(uri, this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            browserOpener = (FileBrowserOpener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(String.format(
                    "%s must implement %s", getActivity().toString(), FileBrowserOpener.class));
        }
    }
}
