package org.spacebison.multimic.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.Analytics;
import org.spacebison.multimic.model.MediaSenderRecorder;
import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.model.RecordListener;
import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.OnDisconnectedListener;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by cmb on 27.10.15.
 */
public class AudioRecordActivity extends Activity {
    private static final String TAG = "cmb.AudioRecordA";
    public static final String EXTRA_SERVER_ADDRESS = "serverAddress";
    public static final String EXTRA_SERVER_PORT = "serverPort";
    private MediaSenderRecorder mMediaSenderRecorder = MediaSenderRecorder.getInstance();;
    private Tracker mTracker;
    private long mRecordStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        MultimicApplication application = (MultimicApplication) getApplication();
        mTracker = application.getDefaultTracker();

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
            }

            @Override
            public void onRecordingFinished() {
                long recordingLength = mRecordStartTime - System.currentTimeMillis();
                mTracker.send(
                        new HitBuilders.EventBuilder(
                                Analytics.CATEGORY_RECORDING,
                                Analytics.ACTION_RECORD_FINISHED)
                                .setValue(recordingLength)
                                .build());
            }
        });

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            InetAddress address = (InetAddress) extras.getSerializable(EXTRA_SERVER_ADDRESS);
            int port = extras.getInt(EXTRA_SERVER_PORT);
            mMediaSenderRecorder.connect(address, port);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Client");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit?");
        builder.setMessage("Exit the server?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaSenderRecorder.release();
    }
}
