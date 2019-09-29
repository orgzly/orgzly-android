package com.orgzly.android.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
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
    private static final String ARG_POSITIVE_BUTTON_TEXT = "pos";
    private static final String ARG_NEGATIVE_BUTTON_TEXT = "neg";
    private static final String ARG_USER_DATA = "bundle";

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SimpleOneLinerDialog.class.getName();

    private Listener mListener;

    private int mId;
    private int mTitle;
    private int mHint;
    private String mValue;
    private int mPositiveButtonText;
    private int mNegativeButtonText;
    private Bundle mUserData;

    public static SimpleOneLinerDialog getInstance(
            int id,
            @StringRes int title,
            @StringRes int hint,
            @StringRes int positiveButtonText,
            @StringRes int negativeButtonText,
            String defaultValue,
            Bundle userData) {

        SimpleOneLinerDialog dialog = new SimpleOneLinerDialog();

        Bundle args = new Bundle();

        args.putInt(ARG_TITLE, title);

        if (hint != 0) {
            args.putInt(ARG_HINT, hint);
        }

        if (defaultValue != null) {
            args.putString(ARG_VALUE, defaultValue);
        }

        args.putInt(ARG_ID, id);

        if (userData != null) {
            args.putBundle(ARG_USER_DATA, userData);
        }

        args.putInt(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        args.putInt(ARG_NEGATIVE_BUTTON_TEXT, negativeButtonText);

        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (Listener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(requireActivity().toString() + " must implement " + Listener.class);
        }


        if (getArguments() == null || !getArguments().containsKey(ARG_TITLE)) {
            throw new IllegalArgumentException(SimpleOneLinerDialog.class.getSimpleName() +
                                               " must have title passed as an argument");
        }

        mId = getArguments().getInt(ARG_ID);
        mTitle = getArguments().getInt(ARG_TITLE);
        mHint = getArguments().getInt(ARG_HINT);
        mValue = getArguments().getString(ARG_VALUE);
        mUserData = getArguments().getBundle(ARG_USER_DATA);

        mPositiveButtonText = getArguments().getInt(ARG_POSITIVE_BUTTON_TEXT);
        mNegativeButtonText = getArguments().getInt(ARG_NEGATIVE_BUTTON_TEXT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.dialog_simple_one_liner, null, false);

        final EditText input = (EditText) view.findViewById(R.id.dialog_input);

        if (mHint != 0) {
            input.setHint(mHint);
        }

        if (mValue != null) {
            input.setText(mValue);
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

        dialog.setOnShowListener(d -> ActivityUtils.openSoftKeyboard(getActivity(), input));

        return dialog;
    }

    public interface Listener {
        void onSimpleOneLinerDialogValue(int id, @NonNull String value, Bundle bundle);
    }
}
