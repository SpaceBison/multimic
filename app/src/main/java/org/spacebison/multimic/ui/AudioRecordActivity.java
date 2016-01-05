package org.spacebison.multimic.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.Analytics;
import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.model.MediaSenderRecorder;
import org.spacebison.multimic.model.RecordListener;
import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.OnDisconnectedListener;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cmb on 27.10.15.
 */
public class AudioRecordActivity extends Activity {
    private static final String TAG = "cmb.AudioRecordA";
    private static final String STATE_RECORD_START_TIME = "record_start_time";
    public static final String EXTRA_SERVER_ADDRESS = "serverAddress";
    public static final String EXTRA_SERVER_PORT = "serverPort";
    private MediaSenderRecorder mMediaSenderRecorder = MediaSenderRecorder.getInstance();
    private ScheduledThreadPoolExecutor mRefreshExecutor;
    private ScheduledFuture<?> mRefreshScheduledFuture;
    private Tracker mTracker;
    private TextView mTimeTextView;
    private long mRecordStartTime = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        mTracker = MultimicApplication.getDefaultTracker();

        mRefreshExecutor = new ScheduledThreadPoolExecutor(1);
        mRefreshExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        mRefreshExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        mTimeTextView = (TextView) findViewById(R.id.recordTime);
        setRecordTimeText(0);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            InetAddress address = (InetAddress) extras.getSerializable(EXTRA_SERVER_ADDRESS);
            int port = extras.getInt(EXTRA_SERVER_PORT);
            mMediaSenderRecorder.connect(address, port);
        } else {
            mRecordStartTime = savedInstanceState.getLong(STATE_RECORD_START_TIME, System.currentTimeMillis());
            setRecordTimeText(System.currentTimeMillis() - mRecordStartTime);
        }
    }

    public void setRecordTimeText(long recordTime) {
        final int hours = (int) (recordTime / 3600000);
        recordTime -= hours * 3600000;
        final int minutes = (int) (recordTime / 60000);
        recordTime -= minutes * 60000;
        final int seconds = (int) (recordTime / 1000);
        recordTime -= seconds * 1000;
        final long millis = recordTime;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimeTextView.setText(String.format("%d:%02d:%02d:%03d", hours, minutes, seconds, millis));
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_RECORD_START_TIME, mRecordStartTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Client");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        mMediaSenderRecorder.setOnConnectedListener(new OnConnectedListener() {
            @Override
            public void onConnected(Socket socket) {
                mTracker.send(
                        new HitBuilders.EventBuilder()
                                .setCategory(Analytics.CATEGORY_CONNECTION)
                                .setAction(Analytics.ACTION_CONNECT)
                                .setLabel(Analytics.LABEL_TO_SERVER)
                                .build());
            }
        });

        mMediaSenderRecorder.setOnDisconnectedListener(new OnDisconnectedListener() {
            @Override
            public void onDisconnected(Socket socket) {
                mTracker.send(
                        new HitBuilders.EventBuilder()
                                .setCategory(Analytics.CATEGORY_CONNECTION)
                                .setAction(Analytics.ACTION_DISCONNECT)
                                .setLabel(Analytics.LABEL_FROM_SERVER)
                                .build());
            }
        });

        mMediaSenderRecorder.setRecordListener(new RecordListener() {
            @Override
            public void onRecordingStarted() {
                mRecordStartTime = System.currentTimeMillis();
                mTracker.send(
                        new HitBuilders.EventBuilder(
                                Analytics.CATEGORY_RECORDING,
                                Analytics.ACTION_RECORD_STARTED).build());
                mRefreshScheduledFuture = mRefreshExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        long recordTime = System.currentTimeMillis() - mRecordStartTime;
                        setRecordTimeText(recordTime);
                    }
                }, 0, 20, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onRecordingFinished() {
                long recordingLength = System.currentTimeMillis() - mRecordStartTime;
                Log.i(TAG, "Finished recording: " + recordingLength + " ms");
                mTracker.send(
                        new HitBuilders.EventBuilder(
                                Analytics.CATEGORY_RECORDING,
                                Analytics.ACTION_RECORD_FINISHED)
                                .setValue(recordingLength)
                                .build());
                mRefreshScheduledFuture.cancel(true);
                mRefreshExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        setRecordTimeText(0);
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRefreshScheduledFuture != null) {
            mRefreshScheduledFuture.cancel(true);
        }
        mRefreshExecutor.execute(new Runnable() {
            @Override
            public void run() {
                setRecordTimeText(0);
            }
        });
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit?");
        builder.setMessage("Disconnect from the server?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mMediaSenderRecorder.release();
                mRefreshExecutor.shutdownNow();
                finish();
                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }
}
