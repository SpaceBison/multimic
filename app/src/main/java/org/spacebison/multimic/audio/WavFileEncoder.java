package org.spacebison.multimic.audio;

import org.spacebison.common.CrashlyticsLog;

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

    public void encode(File input, File output) {
        mExecutor.execute(new EncodeRunnable(input, output));
    }

    private WavFileEncoder() {}

    private class EncodeRunnable implements Runnable {
        private File mInput;
        private File mOutput;

        public EncodeRunnable(File input, File output) {
            mInput = input;
            mOutput = output;
        }

        @Override
        public void run() {
            String wavFileName = mInput.getName();
            int dotIndex = wavFileName.lastIndexOf('.');

            if (dotIndex == -1) {
                wavFileName = wavFileName + ".wav";
            } else {
                wavFileName = wavFileName.substring(0, dotIndex) + ".wav";
            }

            CrashlyticsLog.i(TAG, "Encoding " + mInput.getAbsolutePath() + " as " + mOutput.getAbsolutePath());

            try {
                WavUtils.makeWavFile(mInput, (short)1, 44100, (short)16, mOutput);
            } catch (IOException e) {
                CrashlyticsLog.e(TAG, "Could not encode file " + mInput.getName() + ": " + e);
            }

            CrashlyticsLog.i(TAG, "Encoded file " + mOutput.getAbsolutePath());

            boolean deleted = mInput.delete();

            CrashlyticsLog.i(TAG, "Deleted raw file " + mInput.getName() + ": " + (deleted ? "Success" : "Failure"));
        }
    }
}
