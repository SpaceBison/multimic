package org.spacebison.multimic;

import android.app.Activity;
import android.os.Bundle;

import java.net.InetAddress;

/**
 * Created by cmb on 27.10.15.
 */
public class AudioRecordActivity extends Activity {
    private static final String TAG = "cmb.AudioRecordA";
    public static final String EXTRA_SERVER_ADDRESS = "serverAddress";
    public static final String EXTRA_SERVER_PORT = "serverPort";
    private MediaSenderRecorder mMediaSenderRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        mMediaSenderRecorder = MediaSenderRecorder.getInstance();

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            InetAddress address = (InetAddress) extras.getSerializable(EXTRA_SERVER_ADDRESS);
            int port = extras.getInt(EXTRA_SERVER_PORT);
            mMediaSenderRecorder.connect(address, port);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaSenderRecorder.release();
    }
}
