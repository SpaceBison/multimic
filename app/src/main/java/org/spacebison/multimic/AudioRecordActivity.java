package org.spacebison.multimic;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import org.spacebison.multimic.net.Client;
import org.spacebison.multimic.net.OnCommandListener;
import org.spacebison.multimic.net.Protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by cmb on 27.10.15.
 */
public class AudioRecordActivity extends Activity implements OnCommandListener {
    private static final String TAG = "cmb.AudioRecordA";
    public static final String EXTRA_SERVER_ADDRESS = "serverAddress";
    public static final String EXTRA_SERVER_PORT = "serverPort";
    private MediaRecorder mMediaRecorder;
    private Client mClient;
    private File mFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        Bundle extras = getIntent().getExtras();
        InetAddress address = (InetAddress) extras.getSerializable(EXTRA_SERVER_ADDRESS);
        int port = extras.getInt(EXTRA_SERVER_PORT);

        try {
            mFile = File.createTempFile("multimic", "dupa");

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            mMediaRecorder.setOutputFile(new FileOutputStream(mFile).getFD());
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Error starting client: " + e);
        }

        Log.d(TAG, "Set tmp file: " + mFile.getAbsolutePath());

        mClient = new Client(address, port);
        mClient.setOnCommandListener(this);
        mClient.start();
    }

    @Override
    public void onCommand(byte command) {
        Log.d(TAG, "Got command: " + Integer.toHexString(command));
        try {
            switch (command) {
                case Protocol.START_RECORD:
                    mMediaRecorder.start();
                    mClient.startSending(new FileInputStream(mFile));
                    break;

                case Protocol.STOP_RECORD:
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                    mClient.stopSending();
                    break;

                default:
                    Log.w(TAG, "Unknown command " + Integer.toHexString(command));
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error responding to command: " + e);
        }
    }
}
