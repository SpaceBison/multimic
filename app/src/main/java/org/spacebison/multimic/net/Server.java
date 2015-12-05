package org.spacebison.multimic.net;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cmb on 24.10.15.
 */
public class Server {
    private static final String TAG = "cmb.Server";
    private static final int DEFAULT_THREAD_COUNT = 4;
    private AcceptThread mAcceptThread;
    private final LinkedList<Socket> mClientSockets = new LinkedList<>();
    private ExecutorService mExecutor = Executors.newCachedThreadPool();
    private int mPort;
    private OnConnectedListener mOnConnectedListener;
    private OnDisconnectedListener mOnDisconnectedListener;
    private OnConnectionErrorListener mOnConnectionErrorListener;

    public Server(int port) {
        mPort = port;
    }

    public void start() {
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
        }
        mAcceptThread.start();
    }

    public void disconnect() {
        mAcceptThread.release();
        mAcceptThread = null;
        synchronized (mClientSockets) {
            for (final Socket s : mClientSockets) {
                try {
                    s.close();
                } catch (IOException ignored) {
                }
                onDisconnected(s);
            }
        }
    }

    private void onDisconnected(final Socket s) {
        if (mOnDisconnectedListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnDisconnectedListener.onDisconnected(s);
                }
            });
        }
    }

    public void setOnDisconnectedListener(OnDisconnectedListener mOnDisconnectedListener) {
        this.mOnDisconnectedListener = mOnDisconnectedListener;
    }

    public void setOnConnectedListener(OnConnectedListener onConnectedListener) {
        mOnConnectedListener = onConnectedListener;
    }

    public void setOnConnectionErrorListener(OnConnectionErrorListener onConnectionErrorListener) {
        mOnConnectionErrorListener = onConnectionErrorListener;
    }

    private class AcceptThread extends Thread {

        private ServerSocket mServerSocket;

        public AcceptThread() {
            super(TAG + "AcceptThread");
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting thread: " + getName());
            try {
                mServerSocket = new ServerSocket(mPort);
            } catch (IOException e) {
                Log.e(TAG, "Error starting server: " + e.toString());
                return;
            }

            Log.d(TAG, "Listening: " + mServerSocket);

            while (!isInterrupted()) {
                try {
                    final Socket socket = mServerSocket.accept();

                    Log.d(TAG, "Accepted: " + socket);

                    synchronized (mClientSockets) {
                        mClientSockets.add(socket);
                    }

                    onConnected(socket);
                } catch (final IOException e) {
                    Log.w(TAG, e.toString());
                }
            }

            Log.d(TAG, "Closed");
        }

        public void release() {
            interrupt();
            try {
                mServerSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void onConnected(final Socket socket) {
        if (mOnConnectedListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnConnectedListener.onConnected(socket);
                }
            });
        }
    }
}
