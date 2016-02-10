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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by cmb on 24.10.15.
 */
public class MulticastServiceResolver {
    private static final String TAG = "cmb.MulticastSR";
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);

    private final String mMulticastAddress;
    private final int mMulticastPort;
    private final int mVersion;
    private final Listener mListener;

    public MulticastServiceResolver(String multicastAddress, int multicastPort, Listener listener) {
        this(multicastAddress, multicastPort, 1, listener);
    }

    public MulticastServiceResolver(String multicastAddress, int multicastPort, int version, Listener listener) {
        mMulticastAddress = multicastAddress;
        mMulticastPort = multicastPort;
        mVersion = version;
        mListener = listener;
    }

    public void resolve(final int timeout) {
        mExecutor.execute(new ResolveServiceTask(timeout, false));
    }

    public void resolveOne(final int timeout) {
        mExecutor.execute(new ResolveServiceTask(timeout, true));
    }

    private class ReceiveResponseCall implements Callable<List<ResolvedService>> {
        private final DatagramSocket mSocket;
        private final boolean mResolveOne;
        private final int mTimeout;

        public ReceiveResponseCall(DatagramSocket socket, boolean resolveOne, int timeout) {
            mSocket = socket;
            mResolveOne = resolveOne;
            mTimeout = timeout;
        }

        @Override
        public List<ResolvedService> call() throws Exception {
            LinkedList<ResolvedService> resolvedServices = new LinkedList<>();
            long endTime = System.currentTimeMillis() + mTimeout;

            DatagramPacket packet = new DatagramPacket(new byte[Common.MAX_PACKET_SIZE], Common.MAX_PACKET_SIZE);

            try {
                mSocket.setSoTimeout(mTimeout);
            } catch (IOException e) {
                Log.e(TAG, "Error setting timeout: " + e);
                return null;
            }

            while (!Thread.interrupted() && System.currentTimeMillis() < endTime) {
                try {
                    mSocket.receive(packet);

                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));
                    ResolvedService service = (ResolvedService) ois.readObject();
                    ois.close();

                    Log.d(TAG, "Received response from " + packet.getAddress() + ": " + service);

                    resolvedServices.add(service);

                    if (mResolveOne) {
                        return resolvedServices;
                    }
                } catch (IOException e) {
                    Log.w(TAG, e.toString());
                }
            }

            return resolvedServices;
        }
    }

    private class ResolveServiceTask implements Runnable {
        private final boolean mResolveOne;
        private final int mTimeout;

        public ResolveServiceTask(int timeout, boolean resolveOne) {
            mTimeout = timeout;
            mResolveOne = resolveOne;
        }

        @Override
        public void run() {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.connect(InetAddress.getByName(mMulticastAddress), mMulticastPort);
            } catch (UnknownHostException | SocketException e) {
                Log.e(TAG, "Could not connect to multicast " + mMulticastAddress + ':' + mMulticastPort);
                return;
            }

            Future<List<ResolvedService>> resolvedServicesFuture =
                    mExecutor.submit(new ReceiveResponseCall(socket, mResolveOne, mTimeout));

            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                new ObjectOutputStream(bos).writeObject(new DiscoveryRequest(mVersion));
                byte[] responseBytes = bos.toByteArray();

                socket.send(new DatagramPacket(responseBytes, responseBytes.length));
                bos.close();
            } catch (IOException e) {
                Log.e(TAG, "Error sending request: " + e);
            }

            List<ResolvedService> resolvedServices = null;
            try {
                 resolvedServices = resolvedServicesFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            mListener.onServicesResolved(resolvedServices);
        }
    }

    public interface Listener {
        void onServicesResolved(List<ResolvedService> services);
    }
}
