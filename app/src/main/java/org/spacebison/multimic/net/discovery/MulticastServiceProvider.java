package org.spacebison.multimic.net.discovery;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by cmb on 24.10.15.
 */
public class MulticastServiceProvider {
    private static final String TAG = "cmb.MutlicastSP";
    private BroadcastListenerThread mThread = new BroadcastListenerThread();
    private String mAddress;
    private int mServicePort;
    private int mPort;
    private OnRequestReceivedListener mOnRequestReceivedListener;

    public MulticastServiceProvider(String multicastAddress, int multicastPort, int servicePort) {
        mPort = multicastPort;
        mAddress = multicastAddress;
        mServicePort = servicePort;
    }

    public void start() {
        mThread.start();
    }

    public void stop() {
        mThread.interrupt();
    }

    public void setOnRequestReceivedListener(OnRequestReceivedListener onRequestReceivedListener) {
        mOnRequestReceivedListener = onRequestReceivedListener;
    }

    private class BroadcastListenerThread extends Thread {
        public BroadcastListenerThread() {
            super(TAG + "ListenerThread");
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting thread " + getName());

            MulticastSocket socket;
            try {
                socket = new MulticastSocket(mPort);
                socket.joinGroup(InetAddress.getByName(mAddress));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Log.d(TAG, "Listening: " + mAddress + ':' + socket.getLocalPort());

            while(!isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1], 1);
                    socket.receive(packet);
                    Log.d(TAG, "Received request from: " + packet.getAddress());

                    if (mOnRequestReceivedListener != null) {
                        mOnRequestReceivedListener.onRequestReceived(packet);
                    }

                    DatagramSocket clientSocket = new DatagramSocket();
                    clientSocket.connect(packet.getAddress(), packet.getPort());
                    DiscoveryResponse discoveryResponse = new DiscoveryResponse(socket.getNetworkInterface().getInetAddresses().nextElement(), mServicePort);
                    Log.d(TAG, "Sending response: " + discoveryResponse);
                    byte[] responseBytes = discoveryResponse.getBytes();
                    clientSocket.send(new DatagramPacket(responseBytes, responseBytes.length));
                    clientSocket.close();
                } catch (IOException e) {
                    Log.w(TAG, e.toString());
                }
                yield();
            }
            socket.close();
        }
    }
}
