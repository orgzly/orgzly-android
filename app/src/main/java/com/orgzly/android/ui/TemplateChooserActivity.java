package com.orgzly.android.ui;

import android.content.Intent;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.BookUtils;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.ui.share.ShareActivity;

public class TemplateChooserActivity extends BookChooserActivity {
    @Override
    public void onBookClicked(long bookId) {
        if (action == null) {
            return;
        }
        if (!action.equals(Intent.ACTION_CREATE_SHORTCUT)) {
            return;
        }
        Book book = dataRepository.getBook(bookId);

        if (book == null) {
            Toast.makeText(this, R.string.book_does_not_exist_anymore, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String id = "template-" + bookId;
        String name = book.getName();
        String title = BookUtils.getFragmentTitleForBook(book);
        Intent launchIntent = ShareActivity.createNewNoteIntent(this);
        launchIntent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId);
        IconCompat icon = createIcon();

        ShortcutInfoCompat shortcut =
                new ShortcutInfoCompat.Builder(this, id)
                        .setShortLabel(name)
                        .setLongLabel(title)
                        .setIcon(icon)
                        .setIntent(launchIntent)
                        .build();

        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, shortcut));

        finish();
    }

    private IconCompat createIcon() {
        return IconCompat.createWithResource(this, R.mipmap.cic_shortcut_new_note);
    }
}
