package com.orgzly.android.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.orgzly.R;

public class WhatsNewDialog {
    /**
     * Display dialog with changes.
     */
    public static AlertDialog create(Context context) {
        @SuppressLint("InflateParams") final View layoutView =
                ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.dialog_whats_new, null, false);

        TextView view = ((TextView) layoutView.findViewById(R.id.dialog_whats_new_follow));
        view.setText(fromHtml(context.getString(R.string.whats_new_follow)));
        view.setMovementMethod(LinkMovementMethod.getInstance());

        return new AlertDialog.Builder(context)
                .setTitle(R.string.whats_new_title)
                .setPositiveButton(R.string.ok, null)
                .setView(layoutView)
                .create();
    }

    @SuppressWarnings("deprecation")
    private static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }
}
