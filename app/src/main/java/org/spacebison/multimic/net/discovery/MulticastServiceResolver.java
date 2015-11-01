package org.spacebison.multimic.net.discovery;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by cmb on 24.10.15.
 */
public class MulticastServiceResolver {
    private static final String TAG = "cmb.MulticastSR";

    private String mMulticastAddress;
    private int mMulticastPort;
    private OnServiceResolvedListener mListener;

    public MulticastServiceResolver(String multicastAddress, int multicastPort, OnServiceResolvedListener listener) {
        mMulticastAddress = multicastAddress;
        mMulticastPort = multicastPort;
        mListener = listener;
    }

    public void resolve(final int timeout) {
        final long startTime = System.currentTimeMillis();
        new Thread(TAG + "Thread") {
            @Override
            public void run() {
                Log.d(TAG, "Starting thread: " + getName());
                DatagramSocket socket = null;
                Log.d(TAG, "Sending request to " + mMulticastAddress + ':' + mMulticastPort);
                try {
                    socket = new DatagramSocket();
                    socket.connect(InetAddress.getByName(mMulticastAddress), mMulticastPort);

                    socket.send(new DatagramPacket(new byte[1], 1));
                    InetAddress address = socket.getLocalAddress();
                    int port = socket.getLocalPort();
                    socket.close();
                    socket = new DatagramSocket(port, address);
                    socket.setSoTimeout(timeout);
                    Log.d(TAG, "Listening for response on " + address + ':' + port);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                while (System.currentTimeMillis() - startTime < timeout) {
                    try {
                        DatagramPacket packet = new DatagramPacket(new byte[DiscoveryResponse.maxByteCount()], DiscoveryResponse.maxByteCount());
                        socket.receive(packet);
                        Log.d(TAG, "Received response from: " + packet.getAddress());
                        DiscoveryResponse response = DiscoveryResponse.fromBytes(packet.getData());
                        mListener.onServiceResolved(packet.getAddress(), response.getPort());
                    } catch (IOException e) {
                        Log.w(TAG, e.toString());
                    }
                    yield();
                }

                socket.close();
            }
        }.start();
    }
}
