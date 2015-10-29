package org.spacebison.multimic.audio;

import android.util.Log;

import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.Protocol;
import org.spacebison.multimic.net.Server;
import org.spacebison.multimic.net.discovery.MulticastServiceProvider;
import org.spacebison.multimic.net.discovery.OnRequestReceivedListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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

    public void start() {
        mServer.start();
        mServiceProvider.start();
    }

    public void stop() {
        mServer.disconnect();
        mServiceProvider.stop();
    }

    public void setOnConnectedListener(OnConnectedListener onConnectedListener) {
        mServer.setOnConnectedListener(onConnectedListener);
    }

    public void setOnRequestReceivedListener(OnRequestReceivedListener onRequestReceivedListener) {
        mServiceProvider.setOnRequestReceivedListener(onRequestReceivedListener);
    }

    private class ClientConnectionSession extends Thread {
        private Socket mSocket;
        private OutputStream mOutput;

        public ClientConnectionSession(Socket socket, OutputStream output) {
            mSocket = socket;
            mOutput = output;
        }

        @Override
        public void run() {
            InputStream input = null;
            try {
                mSocket.sendUrgentData(Protocol.START_RECORD);
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
                }
            }
        }
    }
}
