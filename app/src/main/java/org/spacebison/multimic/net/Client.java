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
    private OnConnectedListener mOnConnectedListener;
    private OnConnectionErrorListener mErrorListener;
    private OnCommandListener mOnCommandListener;

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

    public void startSending(InputStream inputStream) {
        Log.d(TAG, "Start sending");
        if (mSendThread == null || mSendThread.isAlive()) {
            return;
        }

        mSendThread = new SendThread(inputStream);
        mSendThread.start();
    }

    public void stopSending() {
        Log.d(TAG, "Stop sending");
        if (mSendThread != null) {
            mSendThread.interrupt();
        }
    }

    public void disconnect() {
        mClientThread.interrupt();
        try {
            mSocket.close();
        } catch (IOException ignored) {
        }
    }

    public void setOnCommandListener(OnCommandListener onCommandListener) {
        mOnCommandListener = onCommandListener;
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
                    Log.d(TAG, "Got command: " + Integer.toHexString(b));
                    if (mOnCommandListener != null) {
                        mOnCommandListener.onCommand(b);
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
            Log.d(TAG, "Ending");
        }
    }

    private class SendThread extends Thread {
        InputStream mInputStream;

        public SendThread(InputStream inputStream) {
            mInputStream = inputStream;
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting send thread");
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
            Log.d(TAG, "Finished sending");
        }
    }
}
