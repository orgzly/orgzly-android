package com.orgzly.android.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.orgzly.R;

public class WhatsNewDialog {
    /**
     * Display dialog with changes.
     */
    public static AlertDialog create(Activity activity) {
        @SuppressLint("InflateParams") final View layoutView =
                activity.getLayoutInflater().inflate(R.layout.dialog_whats_new, null, false);

        TextView view;

        view = ((TextView) layoutView.findViewById(R.id.dialog_whats_new_info));
        view.setText(fromHtml(activity.getString(R.string.whats_new_info)));
        view.setMovementMethod(LinkMovementMethod.getInstance());

        view = ((TextView) layoutView.findViewById(R.id.dialog_whats_new_follow));
        view.setText(fromHtml(activity.getString(R.string.whats_new_follow)));
        view.setMovementMethod(LinkMovementMethod.getInstance());

        return new AlertDialog.Builder(activity)
                .setTitle(R.string.whats_new_title)
                .setPositiveButton(R.string.ok, null)
                .setView(layoutView)
                .create();
    }

    @SuppressWarnings("deprecation")
    private static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }
}
