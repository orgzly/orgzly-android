package com.orgzly.android.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.orgzly.R;
import com.orgzly.android.ui.util.ActivityUtils;

public class SimpleOneLinerDialog extends DialogFragment {
    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_HINT = "hint";
    private static final String ARG_VALUE = "value";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_POSITIVE_BUTTON_TEXT = "pos";
    private static final String ARG_NEGATIVE_BUTTON_TEXT = "neg";
    private static final String ARG_USER_DATA = "bundle";

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SimpleOneLinerDialog.class.getName();

    private SimpleOneLinerDialogListener mListener;

    private int mId;
    private String mTitle;
    private String mHint;
    private String mValue;
    private String mDescription;
    private String mPositiveButtonText;
    private String mNegativeButtonText;
    private Bundle mUserData;

    public static SimpleOneLinerDialog getInstance(int id, String title, String hint, String value, String description, String positiveButtonText, String negativeButtonText, Bundle userData) {
        SimpleOneLinerDialog dialog = new SimpleOneLinerDialog();

        Bundle args = new Bundle();

        if (title != null) {
            args.putString(ARG_TITLE, title);
        }

        if (hint != null) {
            args.putString(ARG_HINT, hint);
        }

        if (value != null) {
            args.putString(ARG_VALUE, value);
        }

        if (description != null) {
            args.putString(ARG_DESCRIPTION, description);
        }

        args.putInt(ARG_ID, id);

        if (userData != null) {
            args.putBundle(ARG_USER_DATA, userData);
        }

        args.putString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        args.putString(ARG_NEGATIVE_BUTTON_TEXT, negativeButtonText);

        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = getActivity();

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (SimpleOneLinerDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + SimpleOneLinerDialogListener.class);
        }


        if (getArguments() == null || !getArguments().containsKey(ARG_TITLE)) {
            throw new IllegalArgumentException(SimpleOneLinerDialog.class.getSimpleName() +
                                               " must have title passed as an argument");
        }

        mId = getArguments().getInt(ARG_ID);
        mTitle = getArguments().getString(ARG_TITLE);
        mHint = getArguments().getString(ARG_HINT);
        mValue = getArguments().getString(ARG_VALUE);
        mDescription = getArguments().getString(ARG_DESCRIPTION);
        mUserData = getArguments().getBundle(ARG_USER_DATA);

        mPositiveButtonText = getArguments().getString(ARG_POSITIVE_BUTTON_TEXT);
        mNegativeButtonText = getArguments().getString(ARG_NEGATIVE_BUTTON_TEXT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.dialog_simple_one_liner, null, false);

        final EditText input = (EditText) view.findViewById(R.id.dialog_input);

        if (mHint != null) {
            input.setHint(mHint);
        }

        if (mValue != null) {
            input.setText(mValue);
        }


        TextView description = (TextView) view.findViewById(R.id.dialog_description);
        if (mDescription != null) {
            description.setText(mDescription);
            description.setVisibility(View.VISIBLE);
        } else {
            description.setVisibility(View.GONE);
        }

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(mTitle)
                .setView(view)
                .setPositiveButton(mPositiveButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (!TextUtils.isEmpty(input.getText())) {
                            mListener.onSimpleOneLinerDialogValue(mId, input.getText().toString().trim(), mUserData);
                        }

                        /* Closing due to used android:windowSoftInputMode="stateUnchanged" */
                        ActivityUtils.closeSoftKeyboard(getActivity());
                    }
                })
                .setNegativeButton(mNegativeButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Closing due to used android:windowSoftInputMode="stateUnchanged" */
                        ActivityUtils.closeSoftKeyboard(getActivity());
                    }
                })
                .create();

        /* Perform positive button click on keyboard action press. */
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
        });

        ActivityUtils.openSoftKeyboard(getActivity(), input);

        return dialog;
    }

    public interface SimpleOneLinerDialogListener {
        void onSimpleOneLinerDialogValue(int id, String value, Bundle bundle);
    }
}
