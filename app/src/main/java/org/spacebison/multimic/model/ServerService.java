package org.spacebison.multimic.model;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import org.spacebison.common.CrashlyticsLog;
import org.spacebison.common.Util;
import org.spacebison.multimic.Prefs;
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
import java.util.ArrayList;
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
public class ServerService extends Service implements ListeningServer.Listener, Handler.Callback {
    private static final String TAG = "ServerService";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    private static final File REC_DIR = new File(Environment.getExternalStorageDirectory(), "MultiMic");
    private static final File TMP_DIR = new File(REC_DIR, ".tmp");

    static {
        REC_DIR.mkdirs();
        TMP_DIR.mkdirs();
    }

    private final HandlerThread mHandlerThread = new HandlerThread("Server");
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final ListeningServer mServer = new ListeningServer(Config.SERVER_PORT);
    private final HashMap<Socket, Client> mClients = new HashMap<>();
    private final HashMap<Client, Future> mClientSessionFutures = new HashMap<>();
    private final HashMap<Client, File> mClientTmpFiles = new HashMap<>();
    private Messenger mMessenger;
    private String mName;
    private MulticastServiceProvider mServiceProvider;

    public static IntentFilter getIntentFilter(Context context) {
        IntentFilter filter = new IntentFilter();
        for (Action action : Action.values()) {
            filter.addAction(Util.getFullName(context, action));
        }
        return filter;
    }

    public static void start(Context context) {
        context.startService(new Intent(context, ServerService.class));
    }

    public static void startRecording(Messenger service) throws RemoteException {
        Message msg = Message.obtain();
        msg.what = MessageWhat.START_RECORDING.ordinal();
        service.send(msg);
    }

    public static void stopRecording(Messenger service) throws RemoteException {
        Message msg = Message.obtain();
        msg.what = MessageWhat.STOP_RECORDING.ordinal();
        service.send(msg);
    }

    @Override
    public void onClientConnected(final Socket socket) {
        try {
            socket.setKeepAlive(true);

            CrashlyticsLog.d(TAG, "Saying hello");
            new WriteJsonTask(socket.getOutputStream(), new Hello(mName)).run();

            CrashlyticsLog.d(TAG, "Waiting for a hello from client");
            Hello hello = new ReadJsonCall<>(socket.getInputStream(), Hello.class).call();

            CrashlyticsLog.d(TAG, "Sending NTP request");
            new WriteJsonTask(socket.getOutputStream(), new NtpRequest(System.currentTimeMillis())).run();

            CrashlyticsLog.d(TAG, "Awaiting NTP response");
            NtpResponse ntpResponse = new ReadNtpResponseCall(socket.getInputStream()).call();

            long offset = 0;
            long delay = 0;
            if (ntpResponse != null) {
                offset = ntpResponse.getOffset();
                delay = ntpResponse.getDelay();
            }

            CrashlyticsLog.d(TAG, "Handshake complete");

            String clientName = hello.name;

            final Client newClient = new Client(socket, clientName, offset, delay);

            synchronized (mClients) {
                for (Client client : mClients.values()) {
                    if (client.name.equals(clientName)) {
                        CrashlyticsLog.e(TAG, "Duplicate client name: " + clientName);
                        socket.close();
                        return;
                    }
                }

                mClients.put(newClient.socket, newClient);

                notifyClientConnected(newClient);
            }
        } catch (IOException e) {
            CrashlyticsLog.e(TAG, "Error accepting client: " + e);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void onListeningError(Exception e) {
        CrashlyticsLog.e(TAG, "Listening error: " + e);
    }

    public Set<Client> getClients() {
        return new HashSet<>(mClients.values());
    }

    @Override
    public void onCreate() {
        CrashlyticsLog.d(TAG, "Create");
        super.onCreate();
        mHandlerThread.start();
        mMessenger = new Messenger(new Handler(mHandlerThread.getLooper(), this));
        mServer.setListener(this);
        mName = Prefs.getInstance().getName();
        mServiceProvider = new MulticastServiceProvider(mName, Config.DISCOVERY_MULTICAST_GROUP, Config.DISCOVERY_MULTICAST_PORT, Config.SERVER_PORT);
        mServiceProvider.start();
        mServer.start();
        ClientService.start(this, mServiceProvider.getResovledService());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        CrashlyticsLog.d(TAG, "Destroy");
        super.onDestroy();

        disconnectAllClients();

        mServer.stop();
        mServiceProvider.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public boolean handleMessage(Message msg) {
        MessageWhat what = MessageWhat.values()[msg.what];
        CrashlyticsLog.d(TAG, "Message: " + what);
        switch (what) {
            case START_RECORDING:
                startRecord();
                return true;

            case STOP_RECORDING:
                stopRecord();
                return true;

            default:
                return false;
        }
    }

    private void disconnectAllClients() {
        for (Client client : mClients.values()) {
            try {
                client.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            notifyClientDisconnected(client);
        }

        mClients.clear();
    }

    private void startRecord() {
        CrashlyticsLog.d(TAG, "Start recording");
        synchronized (mClients) {
            CrashlyticsLog.d(TAG, "Starting data transfer sessions");

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
                            mClients.remove(client.socket);
                            notifyClientDisconnected(client);
                        }

                        @Override
                        public void onStreamForwardError() {
                            mClients.remove(client.socket);
                            notifyClientDisconnected(client);
                        }
                    });

                    mClientSessionFutures.put(client, mExecutor.submit(streamForwardTask));
                } catch (IOException e) {
                    CrashlyticsLog.e(TAG, "Error preparing temporary file for client: " + client.name + ": " + e);
                }
            }

            CrashlyticsLog.d(TAG, "Sending commands");

            long maxClientDelay = getMaxClientDelay();
            CrashlyticsLog.d(TAG, "Max client delay: " + maxClientDelay);
            long startTime = System.currentTimeMillis() + maxClientDelay;
            CrashlyticsLog.d(TAG, "Start time: " + new Date(startTime));

            for (Client client : mClients.values()) {
                try {
                    final StartRecord startRecord = new StartRecord(startTime + client.timeOffset);
                    CrashlyticsLog.d(TAG, "Sending: " + startRecord + " to " + client.name);
                    mExecutor.execute(new WriteJsonTask(
                            client.socket.getOutputStream(),
                            startRecord));
                } catch (IOException e) {
                    CrashlyticsLog.d(TAG, "Error sending start record command to client: " + client.name + ": " + e);
                }
            }
        }
    }

    private void stopRecord() {
        CrashlyticsLog.d(TAG, "Stop recording");

        synchronized (mClients) {
            CrashlyticsLog.d(TAG, "Sending stop commands");

            long maxClientDelay = getMaxClientDelay();
            CrashlyticsLog.d(TAG, "Max client delay: " + maxClientDelay);
            long stopTime = System.currentTimeMillis() + maxClientDelay;
            CrashlyticsLog.d(TAG, "Stop time: " + new Date(stopTime));

            for (Client client : mClients.values()) {
                try {
                    mExecutor.execute(new WriteJsonTask(
                            client.socket.getOutputStream(),
                            new StopRecord(stopTime + client.timeOffset)));
                } catch (IOException e) {
                    CrashlyticsLog.d(TAG, "Error sending stop record command to client: " + client.name + ": " + e);
                }
            }

            CrashlyticsLog.d(TAG, "Stopping sessions");

            for (final Client client : mClients.values()) {
                final Future future = mClientSessionFutures.get(client);
                if (future != null) {
                    future.cancel(true);
                }

                CrashlyticsLog.d(TAG, "Submitting WAV encoding task for " + client);
                final File tmpFile = mClientTmpFiles.get(client);
                if (tmpFile != null) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            WavFileEncoder.getInstance().encode(tmpFile, new File(tmpFile.getParentFile().getParentFile(), tmpFile.getName() + ".wav"));
                        }
                    });
                }
            }
        }
    }

    private void notifyClientDisconnected(final Client client) {
        CrashlyticsLog.i(TAG, "Client disconnected: " + client);
        Intent intent = new Intent(Util.getFullName(this, Action.CLIENT_DISCONNECTED));
        intent.putExtra(Util.getFullName(this, Extra.CLIENT), client.name);
        sendBroadcast(intent);
        notifyClientListChanged();
    }

    private void notifyClientConnected(final Client client) {
        CrashlyticsLog.i(TAG, "Client connected: " + client);
        Intent intent = new Intent(Util.getFullName(this, Action.CLIENT_CONNECTED));
        intent.putExtra(Util.getFullName(this, Extra.CLIENT), client.name);
        sendBroadcast(intent);
        notifyClientListChanged();
    }

    private void notifyClientListChanged() {
        ArrayList<String> mNames = new ArrayList<>(mClients.size());

        for (Client client : mClients.values()) {
            mNames.add(client.name);
        }

        Intent intent = new Intent(Util.getFullName(this, Action.CLIENT_LIST_CHANGED));
        intent.putExtra(Util.getFullName(this,Extra. CLIENT_ARRAY), mNames.toArray(new String[mNames.size()]));
        sendBroadcast(intent);
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

    public enum Action {
        CLIENT_CONNECTED,
        CLIENT_DISCONNECTED,
        CLIENT_LIST_CHANGED;
    }

    public enum Extra {
        CLIENT,
        CLIENT_ARRAY;
    }

    public enum MessageWhat {
        START_RECORDING,
        STOP_RECORDING;
    }
}
