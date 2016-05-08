package org.spacebison.multimic.net.discovery;

import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import org.spacebison.common.CrashlyticsLog;

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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
        if (mServiceProviderTaskFuture == null) {
            mServiceProviderTaskFuture = mExecutor.submit(new ServiceProviderTask());
        }
    }

    public void stop() {
        if (mServiceProviderTaskFuture != null) {
            mServiceProviderTaskFuture.cancel(true);
        }
    }

    public ResolvedService getResovledService() {
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException | NetworkOnMainThreadException e) {
            CrashlyticsLog.e(TAG, "Could not get localhost address: " + e);
            CrashlyticsLog.e(TAG, "Trying 127.0.0.1");
            try {
                localHost = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
            } catch (UnknownHostException e1) {
                CrashlyticsLog.wtf(TAG, "Loopback address failed: " + e);
            }
        }
        return buildResolvedService(localHost);
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
                CrashlyticsLog.e(TAG, "Could not start listening: " + e);
                return;
            }

            CrashlyticsLog.d(TAG, "Listening: " + mAddress + ':' + socket.getLocalPort());

            final DatagramPacket packet = new DatagramPacket(new byte[Common.MAX_PACKET_SIZE], Common.MAX_PACKET_SIZE);

            while(!Thread.interrupted()) {
                try {
                    socket.receive(packet);

                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));
                    final DiscoveryRequest request = (DiscoveryRequest) ois.readObject();
                    ois.close();

                    CrashlyticsLog.d(TAG, "Received request from: " + packet.getAddress() + ": " + request);

                    if (request.version > mVersion) {
                        CrashlyticsLog.w(TAG, "Got request for a higher version, dropping");
                        continue;
                    }

                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                //clientSocket.connect(new InetSocketAddress(packet.getAddress(), packet.getPort()));

                                ResolvedService resolvedService = buildResolvedService(socket.getNetworkInterface().getInetAddresses().nextElement());

                                CrashlyticsLog.d(TAG, "Sending response: " + resolvedService + " to " + packet.getAddress() + ':' + packet.getPort());

                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                new ObjectOutputStream(bos).writeObject(resolvedService);
                                byte[] responseBytes = bos.toByteArray();
                                bos.close();

                                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);
                                responsePacket.setAddress(packet.getAddress());
                                responsePacket.setPort(packet.getPort());

                                DatagramSocket clientSocket = new DatagramSocket();
                                clientSocket.send(responsePacket);
                                clientSocket.close();
                            } catch (IOException e) {
                                CrashlyticsLog.e(TAG, "Error sending response: " + e);
                            }
                        }
                    });
                } catch (SocketTimeoutException ignored) {
                } catch (IOException | ClassNotFoundException e) {
                    CrashlyticsLog.w(TAG, "Warning providing service info: " + e);
                }
            }
            socket.close();
            mServiceProviderTaskFuture = null;
        }
    }

    @NonNull
    private ResolvedService buildResolvedService(InetAddress address) {
        return new ResolvedService(mName, mVersion, address, mServicePort);
    }
}
