package org.spacebison.multimic.net;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cmb on 24.10.15.
 */
public class Server {
    private static final String TAG = "cmb.Server";
    private static final int DEFAULT_THREAD_COUNT = 4;
    private final AcceptThread mAcceptThread = new AcceptThread();
    private final LinkedList<Socket> mClientSockets = new LinkedList<>();
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(1, 4, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<Runnable>());
    private int mPort;
    private OnConnectedListener mOnConnectedListener;

    public Server(int port) {
        mPort = port;
    }

    public void setOnConnectedListener(OnConnectedListener onConnectedListener) {
        mOnConnectedListener = onConnectedListener;
    }

    public void start() {
        mAcceptThread.start();
    }

    public void disconnect() {
        mAcceptThread.interrupt();
        synchronized (mClientSockets) {
            for (Socket s : mClientSockets) {
                try {
                    s.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private class AcceptThread extends Thread {
        public AcceptThread() {
            super(TAG + "AcceptThread");
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting thread: " + getName());
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(mPort);
            } catch (IOException e) {
                Log.e(TAG, "Error starting server: " + e.toString());
                return;
            }

            Log.d(TAG, "Listening: " + serverSocket);

            while (!isInterrupted()) {
                try {
                    final Socket socket = serverSocket.accept();

                    Log.d(TAG, "Accepted: " + socket);

                    synchronized (mClientSockets) {
                        mClientSockets.add(socket);
                    }

                    if (mOnConnectedListener != null) {
                        mExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                mOnConnectedListener.onConnected(socket);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
