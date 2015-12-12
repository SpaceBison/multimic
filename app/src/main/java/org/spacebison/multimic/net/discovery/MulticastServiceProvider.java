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
    private BroadcastListenerThread mThread;
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
        if (mThread != null) {
            if (mThread.isAlive()) {
                return;
            } else {
                mThread = new BroadcastListenerThread();
            }
        } else {
            mThread = new BroadcastListenerThread();
        }

        mThread.start();
    }

    public void stop() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    public void setOnRequestReceivedListener(OnRequestReceivedListener onRequestReceivedListener) {
        mOnRequestReceivedListener = onRequestReceivedListener;
    }

    private class BroadcastListenerThread extends Thread {

        private MulticastSocket mSocket;

        public BroadcastListenerThread() {
            super(TAG + "ListenerThread");
        }

        public void release() {
            interrupt();
            mSocket.close();
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting thread " + getName());

            try {
                mSocket = new MulticastSocket(mPort);
                mSocket.joinGroup(InetAddress.getByName(mAddress));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Log.d(TAG, "Listening: " + mAddress + ':' + mSocket.getLocalPort());

            while(!isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1], 1);
                    mSocket.receive(packet);
                    Log.d(TAG, "Received request from: " + packet.getAddress());

                    if (mOnRequestReceivedListener != null) {
                        mOnRequestReceivedListener.onRequestReceived(packet);
                    }

                    sleep(500);

                    DatagramSocket clientSocket = new DatagramSocket();
                    clientSocket.connect(packet.getAddress(), packet.getPort());
                    DiscoveryResponse discoveryResponse = new DiscoveryResponse(mSocket.getNetworkInterface().getInetAddresses().nextElement(), mServicePort);
                    Log.d(TAG, "Sending response: " + discoveryResponse + " to " + packet.getAddress() + ':' + packet.getPort());
                    byte[] responseBytes = discoveryResponse.getBytes();
                    clientSocket.send(new DatagramPacket(responseBytes, responseBytes.length));
                    clientSocket.close();
                } catch (IOException e) {
                    Log.w(TAG, e.toString());
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted");
                }
                yield();
            }
            mSocket.close();
        }
    }
}
