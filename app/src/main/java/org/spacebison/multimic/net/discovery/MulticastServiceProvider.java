package org.spacebison.multimic.net.discovery;

import android.util.Log;

import org.spacebison.multimic.net.discovery.message.DiscoveryRequest;
import org.spacebison.multimic.net.discovery.message.ResolvedService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by cmb on 24.10.15.
 */
public class MulticastServiceProvider {
    private static final String TAG = "cmb.MutlicastSP";
    private static final int TIMEOUT = 30000;
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);
    private final String mName;
    private final String mAddress;
    private final int mServicePort;
    private final int mPort;
    private final int mVersion;
    private Future mServiceProviderTaskFuture;

    public MulticastServiceProvider(String name, String multicastAddress, int multicastPort, int servicePort) {
        this(name, 1, multicastAddress, multicastPort, servicePort);
    }

    public MulticastServiceProvider(String name, int version, String multicastAddress, int multicastPort, int servicePort) {
        mName = name;
        mVersion = version;
        mPort = multicastPort;
        mAddress = multicastAddress;
        mServicePort = servicePort;
    }

    public void start() {
        if (mServiceProviderTaskFuture != null) {
            mServiceProviderTaskFuture = mExecutor.submit(new ServiceProviderTask());
        }
    }

    public void stop() {
        if (mServiceProviderTaskFuture != null) {
            mServiceProviderTaskFuture.cancel(true);
        }
    }


    private class ServiceProviderTask implements Runnable {
        @Override
        public void run() {
            final MulticastSocket socket;
            try {
                socket = new MulticastSocket(mPort);
                socket.joinGroup(InetAddress.getByName(mAddress));
                socket.setSoTimeout(TIMEOUT);
            } catch (IOException e) {
                Log.e(TAG, "Could not start listening: " + e);
                return;
            }

            Log.d(TAG, "Listening: " + mAddress + ':' + socket.getLocalPort());

            final DatagramPacket packet = new DatagramPacket(new byte[Common.MAX_PACKET_SIZE], Common.MAX_PACKET_SIZE);

            while(!Thread.interrupted()) {
                try {
                    socket.receive(packet);

                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));
                    DiscoveryRequest request = (DiscoveryRequest) ois.readObject();
                    ois.close();

                    Log.d(TAG, "Received request from: " + packet.getAddress() + ": " + request);

                    if (request.version > mVersion) {
                        Log.w(TAG, "Got request for a higher version, dropping");
                        continue;
                    }

                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                DatagramSocket clientSocket = new DatagramSocket();
                                clientSocket.connect(packet.getAddress(), packet.getPort());

                                ResolvedService resolvedService = new ResolvedService(mName, mVersion, socket.getNetworkInterface().getInetAddresses().nextElement(), mServicePort);
                                Log.d(TAG, "Sending response: " + resolvedService + " to " + packet.getAddress() + ':' + packet.getPort());

                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                new ObjectOutputStream(bos).writeObject(resolvedService);
                                byte[] responseBytes = bos.toByteArray();

                                clientSocket.send(new DatagramPacket(responseBytes, responseBytes.length));
                                clientSocket.close();
                                bos.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error sending response: " + e);
                            }
                        }
                    });
                } catch (IOException | ClassNotFoundException e) {
                    Log.w(TAG, e.toString());
                }
            }
            socket.close();
        }
    }
}
