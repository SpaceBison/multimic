package org.spacebison.multimic.net;

import org.spacebison.common.CrashlyticsLog;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class creates a server socket and start a new connection accepting thread.
 */
public class ListeningServer {
    private static final String TAG = "cmb.ListeningServer";
    private final ExecutorService mExecutor;
    private AcceptThread mAcceptThread;

    private int mPort;
    private Listener mListener;

    public ListeningServer(int port) {
        this(port, Executors.newCachedThreadPool());
    }

    public ListeningServer(int port, ExecutorService executor) {
        this(port, executor, null);
    }

    public ListeningServer(int port, ExecutorService executor, Listener listener) {
        mPort = port;
        mExecutor = executor;
        mListener = listener;
    }

    public void start() {
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
        }
        mAcceptThread.start();
    }

    public void stop() {
        mAcceptThread.release();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private void onListeningError(final IOException e) {
        if (mListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mListener.onListeningError(e);
                }
            });
        }
    }

    private void onConnected(final Socket socket) {
        if (mListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mListener.onClientConnected(socket);
                }
            });
        } else {
            CrashlyticsLog.d(TAG, "No listener, dropping the new connection: " + socket.getInetAddress());
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }

    public interface Listener {
        void onClientConnected(Socket socket);
        void onListeningError(Exception e);
    }

    private class AcceptThread extends Thread {
        private ServerSocket mServerSocket;

        public AcceptThread() {
            super(TAG + "AcceptThread");
        }

        @Override
        public void run() {
            CrashlyticsLog.d(TAG, "Starting thread: " + getName());
            try {
                mServerSocket = new ServerSocket(mPort);
            } catch (IOException e) {
                CrashlyticsLog.e(TAG, "Error starting server: " + e.toString());
                return;
            }

            CrashlyticsLog.d(TAG, "Listening on port " + mPort);

            while (!isInterrupted()) {
                try {
                    final Socket socket = mServerSocket.accept();
                    CrashlyticsLog.d(TAG, "Accepted: " + socket.getInetAddress());
                    onConnected(socket);
                } catch (final IOException e) {
                    CrashlyticsLog.w(TAG, e.toString());
                    onListeningError(e);
                }
            }

            CrashlyticsLog.d(TAG, "Closed");
        }

        public void release() {
            interrupt();
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
