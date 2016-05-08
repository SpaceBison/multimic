package org.spacebison.multimic.io;

import org.spacebison.common.CrashlyticsLog;

import org.spacebison.multimic.model.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by cmb on 12.02.16.
 */
public class StreamForwardTask implements Runnable {
    private static final String TAG = "StreamForwardTask";
    private final InputStream mInputStream;
    private final OutputStream mOutputStream;
    private Listener mListener;

    public StreamForwardTask(InputStream inputStream, OutputStream outputStream) {
        mInputStream = inputStream;
        mOutputStream = outputStream;
    }

    @Override
    public void run() {
        boolean stop = false;
        int bytes = 0;
        final byte[] buffer = new byte[Config.BUFFER_SIZE];
        try {
            while ((bytes = mInputStream.read(buffer)) >= 0 || !stop) {
                if (bytes == 0) {
                    continue;
                }

                mOutputStream.write(buffer, 0, bytes);
                stop |= Thread.interrupted();
            }
            mOutputStream.flush();

            if (mListener != null) {
                mListener.onStreamForwardFinished();
            }
        } catch (IOException e) {
            CrashlyticsLog.e(TAG, "Error forwarding " + mInputStream + " to " + mOutputStream);
            if (mListener != null) {
                mListener.onStreamForwardError();
            }
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        void onStreamForwardFinished();
        void onStreamForwardError();
    }
}
