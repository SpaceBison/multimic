package org.spacebison.multimic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by cmb on 24.10.15.
 */
public class StreamRedirector {
    private static final String TAG = "cmb.StreamRedirector";
    private StreamRedirectThread mThread = new StreamRedirectThread();
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private int mBufferSize = 1024;
    private boolean mStopOnEof = true;
    private StreamRedirectorListener mListener;

    public StreamRedirector(InputStream inputStream, OutputStream outputStream) {
        mInputStream = inputStream;
        mOutputStream = outputStream;
    }

    public void setBufferSize(int bufferSize) {
        mBufferSize = bufferSize;
    }

    public void setStopOnEof(boolean stopOnEof) {
        mStopOnEof = stopOnEof;
    }

    public void setListener(StreamRedirectorListener listener) {
        mListener = listener;
    }

    public void start() {
        mThread.start();
    }

    public void stop() {
        mThread.interrupt();
    }

    private interface StreamRedirectorListener {
        void onDataTransmitted(long bytes);
        void onTransferCompleted(long bytes);
    }

    private class StreamRedirectThread extends Thread {
        public StreamRedirectThread() {
            super(TAG + "Thread");
        }

        @Override
        public void run() {
            byte[] buf = new byte[mBufferSize];
            int totalBytesRead = 0;
            int bytesRead;
            try {
                while (!interrupted() &&
                        (((bytesRead = mInputStream.read(buf)) >= 0) || !mStopOnEof)) {
                    mOutputStream.write(bytesRead);
                    totalBytesRead += bytesRead;

                    if (mListener != null) {
                        mListener.onDataTransmitted(totalBytesRead);
                    }
                }

                if (mListener != null) {
                    mListener.onTransferCompleted(totalBytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
