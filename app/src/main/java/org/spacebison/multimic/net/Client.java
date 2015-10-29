package org.spacebison.multimic.net;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by cmb on 24.10.15.
 */
public class Client {
    private static final String TAG = "cmb.Client";
    private static final int BUFFER_SIZE = 1024;
    private final Socket mSocket = new Socket();
    private InetAddress mServerAddress;
    private int mServerPort;
    private final ClientThread mClientThread = new ClientThread();
    private SendThread mSendThread;
    private InputStream mInputStream;
    private OnConnectedListener mOnConnectedListener;
    private OnConnectionErrorListener mErrorListener;

    public Client(InetAddress address, int port) {
        mServerAddress = address;
        mServerPort = port;
    }

    public void setOnConnectedListener(OnConnectedListener onConnectedListener) {
        mOnConnectedListener = onConnectedListener;
    }

    public void setErrorListener(OnConnectionErrorListener errorListener) {
        mErrorListener = errorListener;
    }

    public void start() {
        mClientThread.start();
    }

    public void disconnect() {
        mClientThread.interrupt();
        try {
            mSocket.close();
        } catch (IOException ignored) {
        }
    }

    public void setInputStream(InputStream inputStream) {
        mInputStream = inputStream;
    }

    private class ClientThread extends Thread {
        public ClientThread() {
            super(TAG + "Thread");
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting thread");
            try {
                mSocket.connect(new InetSocketAddress(mServerAddress, mServerPort));
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to " + mServerAddress + ':' + mServerPort + ": " + e);
                if (mErrorListener != null) {
                    mErrorListener.onConnectionError(e);
                }
                return;
            }

            Log.d(TAG, "Connected: " + mSocket.getInetAddress());

            if (mOnConnectedListener != null) {
                mOnConnectedListener.onConnected(mSocket);
            }

            InputStream input = null;
            try {
                input = mSocket.getInputStream();
                while (!isInterrupted()) {
                    byte b = (byte) input.read();
                    switch(b) {
                        case Protocol.START_RECORD:
                            mSendThread = new SendThread();
                            mSendThread.start();
                            break;
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "Error receving command: " + e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private class SendThread extends Thread {
        @Override
        public void run() {
            byte[] buf = new byte[BUFFER_SIZE];
            OutputStream output = null;
            try {
                output = mSocket.getOutputStream();
                while (mInputStream.read(buf) >= 0) {
                    output.write(buf);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending: " + e);
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }
}
