package org.spacebison.multimic.audio;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

/**
 * Created by cmb on 24.10.15.
 */
public class AndroidAudioRecorder implements AudioRecorder {
    private static final String TAG = "cmb.AndroidAR";
    private MediaRecorder mMediaRecorder = new MediaRecorder();

    public AndroidAudioRecorder(String filePath) throws IOException {
        mMediaRecorder.reset();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    @Override
    public boolean prepare(String path) {
        mMediaRecorder.setOutputFile(path);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Error initializing recorder: " + e);
            return false;
        }

        return true;
    }

    public void start() throws IllegalStateException {
        mMediaRecorder.start();
    }

    public void stop() throws IllegalStateException {
        mMediaRecorder.stop();
    }

    public void prepare() throws IOException {

        mMediaRecorder.prepare();
    }

    public void release() {
        mMediaRecorder.release();
    }
}
