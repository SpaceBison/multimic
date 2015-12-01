package org.spacebison.multimic.audio;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cmb on 2015-11-11.
 */
public class WavFileEncoder {
    private static WavFileEncoder sInstance;
    private static final Object LOCK = new Object();
    private static final String TAG = "cmb.WavFileEncoder";

    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public static WavFileEncoder getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new WavFileEncoder();
                }
            }
        }
        return sInstance;
    }

    public void encode(File rawFile) {
        mExecutor.execute(new EncodeRunnable(rawFile));
    }

    private WavFileEncoder() {}

    private class EncodeRunnable implements Runnable {
        private File mRawFile;

        public EncodeRunnable(File rawFile) {
            mRawFile = rawFile;
        }

        @Override
        public void run() {
            String wavFileName = mRawFile.getName();
            int dotIndex = wavFileName.lastIndexOf('.');

            if (dotIndex == -1) {
                wavFileName = wavFileName + ".wav";
            } else {
                wavFileName = wavFileName.substring(0, dotIndex) + ".wav";
            }

            File wavFile = new File(mRawFile.getParentFile(), wavFileName);

            Log.i(TAG, "Encoding " + mRawFile.getAbsolutePath() + " as " + wavFile.getAbsolutePath());

            try {
                WavUtils.makeWavFile(mRawFile, (short)1, 44100, (short)16, wavFile);
            } catch (IOException e) {
                Log.e(TAG, "Could not encode file " + mRawFile.getName() + ": " + e);
            }

            Log.i(TAG, "Encoded file " + wavFile.getAbsolutePath());
        }
    }
}
