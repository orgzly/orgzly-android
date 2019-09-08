package com.orgzly.android.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.AppIntent;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.sync.SyncStatus;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.usecase.UseCase;
import com.orgzly.android.usecase.UseCaseResult;
import com.orgzly.android.usecase.UseCaseRunner;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.databinding.FragmentSyncBinding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


/**
 * Retained fragment for user actions.
 */
public class SyncFragment extends Fragment {
    private static final String TAG = SyncFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SyncFragment.class.getName();

    private FragmentSyncBinding binding;

    /** Activity which has this fragment attached. Used as a target for hooks. */
    private Listener mListener;

    private Resources resources;

    /** Progress bar and button text. */
    private SyncButton mSyncButton;

    private BroadcastReceiver syncServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SyncStatus status = SyncStatus.fromIntent(intent);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent, status);

            /* Update sync button based on sync status. */
            if (isAdded()) {
                mSyncButton.update(status);
            }

            switch (status.type) {
                case FAILED:
                    if (mListener != null) {
                        mListener.onSyncFinished(status.message);
                    }
                    break;

                case NO_STORAGE_PERMISSION:
                    Activity activity = getActivity();
                    if (activity != null) {
                        AppPermissions.isGrantedOrRequest((CommonActivity) activity, AppPermissions.Usage.SYNC_START);
                    }
                    break;

                case CANCELED:
                    if (mListener != null) {
                        /* No error message when sync is canceled by the user. */
                        mListener.onSyncFinished(null);
                    }
                    break;
            }
        }
    };


    public static SyncFragment getInstance() {
        return new SyncFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SyncFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
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

        resources = context.getResources();
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(syncServiceReceiver, new IntentFilter(AppIntent.ACTION_SYNC_STATUS));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        binding = FragmentSyncBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retained on configuration change
        mSyncButton = new SyncButton(view, mSyncButton);
    }

    @Override
    public void onStart() {
        super.onStart();

        SyncStatus status = new SyncStatus();
        status.loadFromPreferences(getContext());
        mSyncButton.update(status);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(syncServiceReceiver);
    }

    /**
     * Set the callback to null so we don't accidentally leak the Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        mListener = null;
    }

    /*
     * Sync button which should be updated from the main UI thread.
     */
    private class SyncButton {
        private final Context appContext;

        private final ProgressBar progressBar;

        private final ViewGroup buttonContainer;
        private final TextView buttonText;
        private final View buttonIcon;

        private final Animation rotation;

        public SyncButton(View view, SyncButton prev) {
            this.appContext = requireActivity().getApplicationContext();

            rotation = AnimationUtils.loadAnimation(appContext, R.anim.rotate_counterclockwise);
            rotation.setRepeatCount(Animation.INFINITE);

            progressBar = (ProgressBar) view.findViewById(R.id.sync_progress_bar);

            buttonContainer = (ViewGroup) view.findViewById(R.id.sync_button_container);
            buttonText = (TextView) view.findViewById(R.id.sync_button_text);
            buttonIcon = view.findViewById(R.id.sync_button_icon);

            if (prev != null) {
                /* Restore old state. */
                progressBar.setIndeterminate(prev.progressBar.isIndeterminate());
                progressBar.setMax(prev.progressBar.getMax());
                progressBar.setProgress(prev.progressBar.getProgress());
                progressBar.setVisibility(prev.progressBar.getVisibility());

                buttonText.setText(prev.buttonText.getText());

            } else {
                progressBar.setVisibility(View.INVISIBLE);

                setButtonTextToLastSynced();
            }

            buttonContainer.setOnClickListener(v ->
                    SyncService.start(getContext(), new Intent(getContext(), SyncService.class)));

            buttonContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(getContext())
                            .setPositiveButton(R.string.ok, null)
                            .setMessage(buttonText.getText())
                            .show();

                    return true;
                }
            });
        }

        private void setButtonTextToLastSynced() {
            long time = AppPreferences.lastSuccessfulSyncTime(appContext);

            if (time > 0) {
                buttonText.setText(resources.getString(R.string.last_sync_with_argument, formatLastSyncTime(time)));
            } else {
                buttonText.setText(R.string.sync);
            }
        }

        public void update(SyncStatus status) {
            switch (status.type) {
                case STARTING:
                    progressBar.setIndeterminate(true);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.collecting_notebooks_in_progress);

                    break;

                case CANCELING:
                    progressBar.setIndeterminate(true);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.canceling);

                    break;

                case BOOKS_COLLECTED:
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(status.totalBooks);
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.syncing_in_progress);

                    break;

                case BOOK_STARTED:
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(status.totalBooks);
                    progressBar.setProgress(status.currentBook);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(resources.getString(R.string.syncing_book, status.message));

                    break;

                case BOOK_ENDED:
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(status.totalBooks);
                    progressBar.setProgress(status.currentBook);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.syncing_in_progress);

                    break;

                case NOT_RUNNING:
                case FINISHED:
                    progressBar.setVisibility(View.GONE);

                    setAnimation(false);

                    setButtonTextToLastSynced();

                    break;

                case CANCELED:
                case FAILED:
                    progressBar.setVisibility(View.INVISIBLE);

                    setAnimation(false);

                    buttonText.setText(resources.getString(R.string.last_sync_with_argument, status.message));

                    break;
            }
        }

        private void setAnimation(boolean shouldAnimate) {
            if (shouldAnimate) {
                if (buttonIcon.getAnimation() == null) {
                    buttonIcon.startAnimation(rotation);
                }
            } else {
                if (buttonIcon.getAnimation() != null) {
                    buttonIcon.clearAnimation();
                }
            }
        }

        private String formatLastSyncTime(long time) {
            return DateUtils.formatDateTime(
                    appContext,
                    time,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_TIME);
        }
    }

    public void run(UseCase action) {
        App.EXECUTORS.diskIO().execute(() -> {
            try {
                UseCaseResult result = UseCaseRunner.run(action);

                App.EXECUTORS.mainThread().execute(() -> mListener.onSuccess(action, result));

            } catch (Throwable e) {
                e.printStackTrace();
                App.EXECUTORS.mainThread().execute(() -> mListener.onError(action, e));
            }
        });
    }


    public interface Listener {
        void onSyncFinished(String msg);

        void onSuccess(UseCase action, UseCaseResult result);
        void onError(UseCase action, Throwable throwable);
    }
}
