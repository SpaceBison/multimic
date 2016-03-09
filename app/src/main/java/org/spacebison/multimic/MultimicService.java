package org.spacebison.multimic;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.spacebison.multimic.model.Client;
import org.spacebison.multimic.model.MultimicClient;
import org.spacebison.multimic.model.MultimicServer;
import org.spacebison.multimic.model.Server;
import org.spacebison.multimic.net.discovery.message.ResolvedService;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cmb on 07.02.16.
 */
public class MultimicService extends Service implements MultimicClient.Listener, MultimicServer.Listener {
    private static final String TAG = "MutlimicService";
    private static final int NOTIF_RECORDING = 1488;

    private final ExecutorService mExecutor = Executors.newCachedThreadPool(MultimicApplication.getAnalyticsThreadFactory());
    private final Messenger mMessenger = new Messenger(new ServiceHandler());

    private MultimicServer mMultimicServer;
    private MultimicClient mMultimicClient;
    private int mServerStartId;
    private int mClientStartId;

    public static final String ACTION_CONNECTED = "org.spacebison.multimic.ACTION_CONNECTED";
    public static final String ACTION_DISCONNECTED = "org.spacebison.multimic.ACTION_DISCONNECTED";
    public static final String ACTION_CLIENT_CONNECTED = "org.spacebison.multimic.ACTION_CLIENT_CONNECTED";
    public static final String ACTION_CLIENT_DISCONNECTED = "org.spacebison.multimic.ACTION_CLIENT_DISCONNECTED";
    public static final String EXTRA_CLIENT_NAME = "org.spacebison.multimic.EXTRA_CLIENT_NAME";
    public static final String EXTRA_SERVER_NAME = "org.spacebison.multimic.EXTRA_SERVER_NAME";

    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CONNECTED);
        intentFilter.addAction(ACTION_DISCONNECTED);
        intentFilter.addAction(ACTION_CLIENT_CONNECTED);
        intentFilter.addAction(ACTION_CLIENT_DISCONNECTED);
        return intentFilter;
    }

    public static Message getMessage(MessageCode code) {
        Message message = Message.obtain();
        message.what = code.ordinal();
        return message;
    }

    /*
    public void stopServer() {
        Log.d(TAG, "Stop server");
        if (mMultimicServer != null) {
            stopSelf(mServerStartId);
        }
    }

    public void stopClient() {
        Log.d(TAG, "Stop client");
        if (mMultimicClient != null) {
            stopSelf(mClientStartId);
        }
    }
    */

    public void startRecording() {
        mMultimicServer.startRecording();
    }

    public void stopRecording() {
        mMultimicServer.stopRecording();
    }

    public Set<Client> getClients() {
        return mMultimicServer.getClients();
    }

    public void disconnect() {
        mMultimicClient.disconnect();
    }

    public void release() {
        mMultimicClient.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bind: " + intent);
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Create service");
        super.onCreate();

        mMultimicClient = new MultimicClient(Prefs.getInstance().getName());
        mMultimicClient.setListener(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroy service");
        super.onDestroy();
    }

    @Override
    public void onConnected(Server server) {
        Intent intent = new Intent(ACTION_CONNECTED);
        intent.putExtra(EXTRA_SERVER_NAME, server.name);
        sendBroadcast(intent);
    }

    @Override
    public void onRecordingStarted(long id) {
        Log.d(TAG, "Recording started; id: " + id);
        Notification notification = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_mic_24dp_white)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.recording))
                .build();
        startForeground(NOTIF_RECORDING, notification);
    }

    @Override
    public void onAudioTransferComplete(long id) {
        Log.d(TAG, "Audio transfer complete; id: " + id);
        stopForeground(true);
    }


    @Override
    public void onClientConnected(Client client) {
        Intent intent = new Intent(ACTION_CLIENT_CONNECTED);
        intent.putExtra(EXTRA_CLIENT_NAME, client.name);
        sendBroadcast(intent);
    }

    @Override
    public void onClientDisconnected(Client client) {
        Intent intent = new Intent(ACTION_CLIENT_DISCONNECTED);
        intent.putExtra(EXTRA_CLIENT_NAME, client.name);
        sendBroadcast(intent);
    }

    @SuppressLint("HandlerLeak")
    private class ServiceHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            Log.d(TAG, "Message: " + msg);

            if (msg.what < 0 || msg.what > MessageCode.values.length) {
                return;
            }

            final MessageCode messageCode = MessageCode.values[msg.what];
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    switch (messageCode) {
                        case START_SERVER:
                            if (mMultimicServer != null) {
                                Log.w(TAG, "Server already running");
                                return;
                            }

                            mMultimicServer = new MultimicServer(Prefs.getInstance().getName());
                            mMultimicServer.setListener(MultimicService.this);
                            mMultimicServer.start();
                            mMultimicServer.connectLocalClient(mMultimicClient);
                            break;

                        case START_CLIENT:
                            if (msg.obj instanceof ResolvedService) {
                                mMultimicClient.connect((ResolvedService) msg.obj);
                            }
                            break;

                        case START_RECORDING:
                            mMultimicServer.startRecording();
                            break;

                        case STOP_RECORDING:
                            mMultimicServer.stopRecording();
                            break;

                        default:
                            ServiceHandler.super.handleMessage(msg);
                            break;
                    }
                }
            });
        }
    }

    public enum MessageCode {
        START_SERVER,
        START_CLIENT,
        GET_CLIENTS,
        START_RECORDING,
        STOP_RECORDING,
        TOGGLE_RECORDING;

        public static final MessageCode[] values = values();
    }
}
