package org.spacebison.multimic.model;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.spacebison.multimic.io.AudioRecordInputStream;
import org.spacebison.multimic.net.Client;
import org.spacebison.multimic.net.OnCommandListener;
import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.OnDisconnectedListener;
import org.spacebison.multimic.net.Protocol;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by cmb on 03.11.15.
 */
public class MediaSenderRecorder implements OnCommandListener {
    private static final String TAG = "cmb.MediaSenderRec";
    private static MediaSenderRecorder sInstance;
    private static final Object LOCK = new Object();
    private AudioRecord mAudioRecord;
    private Client mClient;
    private RecordListener mRecordListener;
    private OnConnectedListener mOnConnectedListener;
    private OnDisconnectedListener mOnDisconnectedListener;

    public static MediaSenderRecorder getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new MediaSenderRecorder();
                }
            }
        }
        return sInstance;
    }

    public void connect(InetAddress address, int port) {
        mClient = new Client(address, port);
        mClient.setOnCommandListener(this);
        mClient.setOnConnectedListener(mOnConnectedListener);
        mClient.setOnDisconnectedListener(mOnDisconnectedListener);
        mClient.start();
    }

    public void setOnConnectedListener(OnConnectedListener onConnectedListener) {
        mOnConnectedListener = onConnectedListener;
        if (mClient != null) {
            mClient.setOnConnectedListener(onConnectedListener);
        }
    }

    public void setOnDisconnectedListener(OnDisconnectedListener onDisconnectedListener) {
        mOnDisconnectedListener = onDisconnectedListener;
        if (mClient != null) {
            mClient.setOnDisconnectedListener(onDisconnectedListener);
        }
    }

    public void release() {
        if (mAudioRecord != null) {
            try {
                mAudioRecord.stop();
            } catch (IllegalStateException ignored) {
            }
            mAudioRecord.release();
            mAudioRecord = null;
        }

        mClient.stopSending();
        mClient.disconnect();
    }

    @Override
    public void onCommand(byte command, long whenReceived) {
        long now = System.currentTimeMillis();
        Log.d(TAG, "Got command: " + Integer.toHexString(command));
        switch (command) {
            case Protocol.START_RECORD:
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        44100,
                        1,
                        AudioFormat.ENCODING_PCM_16BIT,
                        mClient.getBufferSize());
                mAudioRecord.startRecording();
                mClient.startSending(new AudioRecordInputStream(mAudioRecord));
                onRecordingStarted();
                break;

            case Protocol.STOP_RECORD:
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
                mClient.stopSending();
                onRecordingFinished();
                break;

            case Protocol.NTP_REQUEST:
                Log.d(TAG, "Got NTP request");
                try {
                    mClient.sendNtpResponse(whenReceived);
                } catch (IOException e) {
                    Log.e(TAG, "Error sending NTP response: " + e);
                }
                break;

            default:
                Log.w(TAG, "Unknown command " + Integer.toHexString(command));
        }
    }

    public void setRecordListener(RecordListener recordListener) {
        mRecordListener = recordListener;
    }

    private void onRecordingStarted() {
        if (mRecordListener != null ) {
            mRecordListener.onRecordingStarted();
        }
    }

    private void onRecordingFinished() {
        if (mRecordListener != null ) {
            mRecordListener.onRecordingFinished();
        }
    }
}
