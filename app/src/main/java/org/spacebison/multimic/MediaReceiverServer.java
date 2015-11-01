package org.spacebison.multimic;

import android.os.Environment;
import android.util.Log;

import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.Protocol;
import org.spacebison.multimic.net.Server;
import org.spacebison.multimic.net.discovery.MulticastServiceProvider;
import org.spacebison.multimic.net.discovery.OnRequestReceivedListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by cmb on 25.10.15.
 */
public class MediaReceiverServer {
    private static final String TAG = "cmb.MediaReceiver";
    private static final int BUFFER_SIZE = 1024;
    private MulticastServiceProvider mServiceProvider =
            new MulticastServiceProvider(
                    Protocol.DISCOVERY_MULTICAST_GROUP,
                    Protocol.DISCOVERY_MULTICAST_PORT,
                    Protocol.SERVER_PORT);
    private Server mServer = new Server(Protocol.SERVER_PORT);
    private LinkedList<Socket> mClients = new LinkedList<>();
    private HashMap<Socket, ClientConnectionSession> mSessions = new HashMap<>();

    public void start() {
        mServer.start();
        mServiceProvider.start();
    }

    public void stop() {
        mServer.disconnect();
        mServiceProvider.stop();
    }

    public void setOnConnectedListener(final OnConnectedListener onConnectedListener) {
        mServer.setOnConnectedListener(new OnConnectedListener() {
            @Override
            public void onConnected(Socket socket) {
                Log.d(TAG, "Connected: " + socket.getInetAddress());
                mClients.add(socket);
                onConnectedListener.onConnected(socket);
            }
        });
    }

    public void setOnRequestReceivedListener(OnRequestReceivedListener onRequestReceivedListener) {
        mServiceProvider.setOnRequestReceivedListener(onRequestReceivedListener);
    }

    public void startReceiving() {
        Log.d(TAG, "Start receiving");
        File sdCard = Environment.getExternalStorageDirectory();
        String dirPath = sdCard.getAbsolutePath() + "/multimic";
        int i = 1;
        final long now = System.currentTimeMillis();
        for (Socket s : mClients) {
            Log.d(TAG, "Creating file for " + s.getInetAddress());
            OutputStream os = null;
            try {
                String fileName = "/rec_" + now + '_' + i++;
                File file = new File(dirPath);
                Log.d(TAG, "Trying dir " + file.getAbsolutePath());
                file.mkdirs();

                file = new File(dirPath + fileName);
                if (file.exists()) {
                    file.delete();
                }

                os = new FileOutputStream(file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error opening file to write: " + e);
                continue;
            }
            ClientConnectionSession session = new ClientConnectionSession(s, os);
            mSessions.put(s, session);
            session.start();
        }
    }

    public void stopReceiving() {
        Log.d(TAG, "Stop receiving");
        for (ClientConnectionSession s : mSessions.values()) {
            s.end();
        }
        mSessions.clear();
    }

    private class ClientConnectionSession extends Thread {
        private Socket mSocket;
        private OutputStream mOutput;
        private OutputStream mClientOutput;

        public ClientConnectionSession(Socket socket, OutputStream output) {
            mSocket = socket;
            mOutput = output;
            try {
                mClientOutput = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void end() {
            try {
                mClientOutput.write(Protocol.STOP_RECORD);
                mClientOutput.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            interrupt();
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting client session: " + mSocket.getInetAddress());
            InputStream input = null;
            try {
                mClientOutput.write(Protocol.START_RECORD);
                mClientOutput.flush();
                input = mSocket.getInputStream();
                byte[] buf = new byte[BUFFER_SIZE];

                while ((input.read(buf) >= 0) && !isInterrupted()) {
                    mOutput.write(buf);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error receiving from " + mSocket.getInetAddress() + ": " + e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }

                    try {
                        mOutput.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        mClientOutput.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG, "Finished session " + mSocket.getInetAddress());
        }
    }
}
