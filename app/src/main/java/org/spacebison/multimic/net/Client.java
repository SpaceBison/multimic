package org.spacebison.multimic.net;

import android.util.Log;

import org.spacebison.multimic.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cmb on 24.10.15.
 */
public class Client {
    private static final String TAG = "cmb.Client";
    private static final int BUFFER_SIZE = 10240;
    private final Socket mSocket = new Socket();
    private InetAddress mServerAddress;
    private int mServerPort;
    private final ClientThread mClientThread = new ClientThread();
    private SendThread mSendThread;
    private OnConnectedListener mOnConnectedListener;
    private OnDisconnectedListener mOnDisconnectedListener;
    private OnConnectionErrorListener mErrorListener;
    private OnCommandListener mOnCommandListener;
    private OnBytesTransferredListener mOnBytesTransferredListener;
    private ExecutorService mUncertainExecutor = Util.newMostCurrentTaskExecutor();
    private ExecutorService mExecutor = Executors.newCachedThreadPool();

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

    public Socket getSocket() {
        return mSocket;
    }

    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    public void startSending(InputStream inputStream) {
        Log.d(TAG, "Start sending");
        if (mSendThread != null && mSendThread.isAlive()) {
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

        onDisconnected();
    }

    public void setOnCommandListener(OnCommandListener onCommandListener) {
        mOnCommandListener = onCommandListener;
    }

    public void setOnBytesTransferredListener(OnBytesTransferredListener onBytesTransferredListener) {
        mOnBytesTransferredListener = onBytesTransferredListener;
    }

    public void setOnDisconnectedListener(OnDisconnectedListener onDisconnectedListener) {
        mOnDisconnectedListener = onDisconnectedListener;
    }

    private class ClientThread extends Thread {
        private static final String TAG = "cmb.ClientThread";

        public ClientThread() {
            super(TAG);
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting thread");
            try {
                mSocket.connect(new InetSocketAddress(mServerAddress, mServerPort));
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to " + mServerAddress + ':' + mServerPort + ": " + e);
                onConnectionError(e);
                return;
            }

            Log.d(TAG, "Connected: " + mSocket.getInetAddress());

            onConnected();

            InputStream input = null;
            try {
                input = mSocket.getInputStream();
                while (!isInterrupted()) {
                    byte b = (byte) input.read();
                    Log.d(TAG, "Got command: " + Integer.toHexString(b));
                    onCommand(b);
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

    private void onDisconnected() {
        if (mOnDisconnectedListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnDisconnectedListener.onDisconnected(mSocket);
                }
            });
        }
    }

    private void onConnected() {
        if (mOnConnectedListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnConnectedListener.onConnected(mSocket);
                }
            });
        }
    }

    private void onCommand(final byte b) {
        if (mOnCommandListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnCommandListener.onCommand(b);
                }
            });
        }
    }

    private void onBytesTransferred(final int bytes) {
        if (mOnBytesTransferredListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnBytesTransferredListener.onBytesTransferred(bytes);
                }
            });
        }
    }

    private class SendThread extends Thread {
        private static final String TAG = "cmb.ClientSendThread";
        InputStream mInputStream;

        public SendThread(InputStream inputStream) {
            super(TAG);
            mInputStream = inputStream;
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting send thread");
            byte[] buf = new byte[BUFFER_SIZE];
            OutputStream output = null;

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                output = mSocket.getOutputStream();
                int byteSum = 0;
                int bytesRead = 0;
                while ((bytesRead = mInputStream.read(buf)) >= 0 || !isInterrupted()) {
                    if (bytesRead > 0) {
                        output.write(buf, 0, bytesRead);
                        byteSum += bytesRead;
                        onBytesTransferred(byteSum);
                    }

                }
                output.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending: " + e);
                onConnectionError(e);
            }
            Log.d(TAG, "Finished sending");
        }
    }

    private void onConnectionError(final IOException e) {
        if (mErrorListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mErrorListener.onConnectionError(mSocket, e);
                }
            });
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes, final int offset, final int count) {
        char[] hexChars = new char[count * 2];
        final int end = offset + count;
        for (int j = offset; j < end; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
