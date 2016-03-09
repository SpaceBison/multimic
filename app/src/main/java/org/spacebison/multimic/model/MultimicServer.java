package org.spacebison.multimic.model;

import android.os.Environment;
import android.util.Log;

import org.spacebison.multimic.audio.WavFileEncoder;
import org.spacebison.multimic.io.StreamForwardTask;
import org.spacebison.multimic.net.ListeningServer;
import org.spacebison.multimic.net.ReadJsonCall;
import org.spacebison.multimic.net.ReadNtpResponseCall;
import org.spacebison.multimic.net.WriteJsonTask;
import org.spacebison.multimic.net.discovery.MulticastServiceProvider;
import org.spacebison.multimic.net.message.Hello;
import org.spacebison.multimic.net.message.NtpRequest;
import org.spacebison.multimic.net.message.NtpResponse;
import org.spacebison.multimic.net.message.StartRecord;
import org.spacebison.multimic.net.message.StopRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by cmb on 10.02.16.
 */
public class MultimicServer implements ListeningServer.Listener {
    private static final String TAG = "MultimicServer";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    private static final File REC_DIR = new File(Environment.getExternalStorageDirectory(), "MultiMic");
    private static final File TMP_DIR = new File(REC_DIR, ".tmp");

    static {
        REC_DIR.mkdirs();
        TMP_DIR.mkdirs();
    }

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final ListeningServer mServer = new ListeningServer(Config.SERVER_PORT);
    private final HashMap<Socket, Client> mClients = new HashMap<>();
    private final HashMap<Client, Future> mClientSessionFutures = new HashMap<>();
    private final HashMap<Client, File> mClientTmpFiles = new HashMap<>();
    private final String mName;
    private final MulticastServiceProvider mServiceProvider;

    private Listener mListener;

    public MultimicServer(String name) {
        mServer.setListener(this);
        mName = name;
        mServiceProvider = new MulticastServiceProvider(name, Config.DISCOVERY_MULTICAST_GROUP, Config.DISCOVERY_MULTICAST_PORT, Config.SERVER_PORT);
    }

    @Override
    public void onClientConnected(final Socket socket) {
        try {
            Log.d(TAG, "Saying hello");
            new WriteJsonTask(socket.getOutputStream(), new Hello(mName)).run();

            Log.d(TAG, "Waiting for a hello from client");
            Hello hello = new ReadJsonCall<>(socket.getInputStream(), Hello.class).call();

            Log.d(TAG, "Sending NTP request");
            new WriteJsonTask(socket.getOutputStream(), new NtpRequest(System.currentTimeMillis())).run();

            Log.d(TAG, "Awaiting NTP response");
            NtpResponse ntpResponse = new ReadNtpResponseCall(socket.getInputStream()).call();

            long offset = 0;
            long delay = 0;
            if (ntpResponse != null) {
                offset = ntpResponse.getOffset();
                delay = ntpResponse.getDelay();
            }

            Log.d(TAG, "Handshake complete");

            String clientName = hello.name;

            final Client newClient = new Client(socket, clientName, offset, delay);

            synchronized (mClients) {
                for (Client client : mClients.values()) {
                    if (client.name.equals(clientName)) {
                        Log.e(TAG, "Duplicate client name: " + clientName);
                        socket.close();
                        return;
                    }
                }

                mClients.put(newClient.socket, newClient);

                notifyClientConnected(newClient);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error accepting client: " + e);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void start() {
        mServiceProvider.start();
        mServer.start();
    }

    public void startRecording() {
        Log.d(TAG, "Start recording");
        synchronized (mClients) {
            Log.d(TAG, "Starting data transfer sessions");

            Date now = new Date();
            for (final Client client : mClients.values()) {
                try {
                    File tmpFile = new File(
                            TMP_DIR, DATE_FORMAT.format(now) + '_' + client.name);

                    mClientTmpFiles.put(client, tmpFile);

                    final StreamForwardTask streamForwardTask = new StreamForwardTask(
                            client.socket.getInputStream(),
                            new FileOutputStream(tmpFile));
                    streamForwardTask.setListener(new StreamForwardTask.Listener() {
                        @Override
                        public void onStreamForwardFinished() {
                            // TODO: 05.03.16 do sth
                        }

                        @Override
                        public void onStreamForwardError() {
                            mClients.remove(client.socket);
                            notifyClientDisconnected(client);
                        }
                    });

                    mClientSessionFutures.put(client, mExecutor.submit(streamForwardTask));
                } catch (IOException e) {
                    Log.e(TAG, "Error preparing temporary file for client: " + client.name + ": " + e);
                }
            }

            Log.d(TAG, "Sending commands");

            long maxClientDelay = getMaxClientDelay();
            Log.d(TAG, "Max client delay: " + maxClientDelay);
            long startTime = System.currentTimeMillis() + maxClientDelay;
            Log.d(TAG, "Start time: " + new Date(startTime));

            for (Client client : mClients.values()) {
                try {
                    final StartRecord startRecord = new StartRecord(startTime + client.timeOffset);
                    Log.d(TAG, "Sending: " + startRecord + " to " + client.name);
                    mExecutor.execute(new WriteJsonTask(
                            client.socket.getOutputStream(),
                            startRecord));
                } catch (IOException e) {
                    Log.d(TAG, "Error sending start record command to client: " + client.name + ": " + e);
                }
            }
        }
    }

    public void stopRecording() {
        Log.d(TAG, "Stop recording");

        synchronized (mClients) {
            Log.d(TAG, "Sending stop commands");

            long maxClientDelay = getMaxClientDelay();
            Log.d(TAG, "Max client delay: " + maxClientDelay);
            long stopTime = System.currentTimeMillis() + maxClientDelay;
            Log.d(TAG, "Stop time: " + new Date(stopTime));

            for (Client client : mClients.values()) {
                try {
                    mExecutor.execute(new WriteJsonTask(
                            client.socket.getOutputStream(),
                            new StopRecord(stopTime + client.timeOffset)));
                } catch (IOException e) {
                    Log.d(TAG, "Error sending stop record command to client: " + client.name + ": " + e);
                }
            }

            Log.d(TAG, "Stopping sessions");

            for (final Client client : mClients.values()) {
                mClientSessionFutures.get(client).cancel(true);
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mClientSessionFutures.get(client);
                        Log.d(TAG, "Submitting WAV encoding task for " + client);
                        final File tmpFile = mClientTmpFiles.get(client);
                        WavFileEncoder.getInstance().encode(tmpFile, new File(tmpFile.getParentFile().getParentFile(), tmpFile.getName() + ".wav"));
                    }
                });
            }
        }
    }

    public Set<Client> getClients() {
        return new HashSet<>(mClients.values());
    }

    public void connectLocalClient(MultimicClient client) {
        // TODO: 14.02.16 add it directly?
        client.connect(mServiceProvider.getResovledService());
    }

    @Override
    public void onListeningError(Exception e) {
        Log.e(TAG, "Listening error: " + e);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private void notifyClientDisconnected(final Client client) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onClientDisconnected(client);
                }
            }
        });
    }

    private void notifyClientConnected(final Client client) {
        if (mListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mListener.onClientConnected(client);
                }
            });
        }
    }

    private long getMaxClientDelay() {
        long maxDelay = 0;
        synchronized (mClients) {
            for (Client client : mClients.values()) {
                if (client.delay > maxDelay) {
                    maxDelay = client.delay;
                }
            }
        }
        return maxDelay;
    }

    public interface Listener {
        void onClientConnected(Client client);
        void onClientDisconnected(Client client);
    }
}
