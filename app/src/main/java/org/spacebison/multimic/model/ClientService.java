package org.spacebison.multimic.model;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import org.spacebison.common.CrashlyticsLog;
import org.spacebison.common.Util;
import org.spacebison.multimic.Prefs;
import org.spacebison.multimic.R;
import org.spacebison.multimic.io.AudioRecordInputStream;
import org.spacebison.multimic.io.StreamForwardTask;
import org.spacebison.multimic.net.ReadJsonCall;
import org.spacebison.multimic.net.ReadNtpRequestCall;
import org.spacebison.multimic.net.WriteJsonTask;
import org.spacebison.multimic.net.discovery.MulticastServiceResolver;
import org.spacebison.multimic.net.discovery.message.ResolvedService;
import org.spacebison.multimic.net.message.Hello;
import org.spacebison.multimic.net.message.Message;
import org.spacebison.multimic.net.message.NtpRequest;
import org.spacebison.multimic.net.message.NtpResponse;
import org.spacebison.multimic.net.message.StartRecord;
import org.spacebison.multimic.net.message.StopRecord;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by cmb on 11.02.16.
 */
public class ClientService extends Service implements Handler.Callback {
    private static final String TAG = "ClientService";
    private static final int NOTIF_ID = 134;

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService mScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final HandlerThread mMessengerThread = new HandlerThread("ClientMessageThread");
    private Messenger mMessenger;
    private String mName;
    private Server mServer;
    private AudioRecord mAudioRecord;
    private Future mStreamForwardFuture;
    private long mRecordId;
    private RecordingState mRecordingState = RecordingState.DISCONNECTED;

    public static void resolveServers(int timeout, MulticastServiceResolver.Listener listener) {
        new MulticastServiceResolver(Config.DISCOVERY_MULTICAST_GROUP, Config.DISCOVERY_MULTICAST_PORT, listener)
                .resolve(timeout);
    }

    public static ComponentName start(Context context, ResolvedService resolvedService) {
        Intent intent = getIntent(context, resolvedService);
        return context.startService(intent);
    }

    public static void requestStateUpdate(Messenger service) throws RemoteException {
        final android.os.Message msg = android.os.Message.obtain();
        msg.what = MessageWhat.UPDATE_STATE.ordinal();
        service.send(msg);
    }

    @NonNull
    public static Intent getIntent(Context context, Parcelable resolvedService) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra(Extra.RESOLVED_SERVICE.name(), resolvedService);
        return intent;
    }

    public static IntentFilter getIntentFilter(Context context) {
        IntentFilter filter = new IntentFilter();
        for (Action action : Action.values()) {
            filter.addAction(Util.getFullName(context, action));
        }
        return filter;
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        MessageWhat what = MessageWhat.values()[msg.what];
        CrashlyticsLog.d(TAG, "Message: " + what);
        switch (what) {
            case UPDATE_STATE:
                broadcastState();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onCreate() {
        CrashlyticsLog.d(TAG, "Create");
        super.onCreate();
        mMessengerThread.start();
        mMessenger = new Messenger(new Handler(mMessengerThread.getLooper(), this));
        mName = Prefs.getInstance().getName();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            init(intent);
            return START_STICKY;
        } else {
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        CrashlyticsLog.d(TAG, "Destroy");
        super.onDestroy();
        releaseAudio();
        disconnect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null) {
            init(intent);
        }
        return mMessenger.getBinder();
    }

    private void init(@NonNull Intent intent) {
        ResolvedService serverService = intent.getParcelableExtra(Extra.RESOLVED_SERVICE.name());
        mExecutor.execute(new ClientTask(serverService));
    }

    private void updateState(RecordingState state) {
        mRecordingState = state;
        broadcastState();
    }

    private void broadcastState() {
        Intent intent = new Intent(Util.getFullName(this, Action.STATE_CHANGED));
        intent.putExtra(Util.getFullName(this, Extra.STATE),  mRecordingState);
        sendBroadcast(intent);
    }

    private void disconnect() {
        if (mServer != null) {
            try {
                mServer.socket.close();
            } catch (IOException ignored) {
            }
            mServer = null;
        }
    }

    private void releaseAudio() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private android.support.v4.app.NotificationCompat.Builder getNotificationBuilder() {
        return new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_mic_white_24dp)
                .setContentTitle(getString(R.string.app_name));
    }

    public enum Action {
        CONNECTED,
        RECORDING_STARTED,
        TRANSFER_COMPLETED,
        STATE_CHANGED;
    }

    public enum Extra {
        RESOLVED_SERVICE,
        SERVER,
        RECORD_ID,
        STATE;
    }

    private class ClientTask implements Runnable {
        ResolvedService mResolvedService;

        public ClientTask(ResolvedService resolvedService) {
            mResolvedService = resolvedService;
        }

        @Override
        public void run() {
            Socket socket = null;
            try {
                socket = new Socket(mResolvedService.address, mResolvedService.port);

                CrashlyticsLog.d(TAG, "Waiting for a hello from server");
                Hello hello = new ReadJsonCall<>(socket.getInputStream(), Hello.class).call();

                CrashlyticsLog.d(TAG, "Saying hello");
                new WriteJsonTask(socket.getOutputStream(), new Hello(mName)).run();

                CrashlyticsLog.d(TAG, "Awaiting NTP request");
                NtpRequest ntpRequest = new ReadNtpRequestCall(socket.getInputStream()).call();

                CrashlyticsLog.d(TAG, "Sending NTP response");
                new WriteJsonTask(socket.getOutputStream(), new NtpResponse(ntpRequest, System.currentTimeMillis())).run();

                CrashlyticsLog.d(TAG, "Handshake complete");

                mServer = new Server(socket, hello.name);

                Intent connectedBroadcast = new Intent(Action.CONNECTED.name());
                connectedBroadcast.putExtra(Extra.SERVER.name(), mServer.name);
                sendBroadcast(connectedBroadcast);
            } catch (Exception e) {
                CrashlyticsLog.e(TAG, "Error connecting to server: " + e);
                disconnect();
                return;
            }

            updateState(RecordingState.CONNECTED);

            ReadJsonCall<Message> readJsonCall;
            try {
                readJsonCall = new ReadJsonCall<>(socket.getInputStream(), Message.class);
            } catch (IOException e) {
                CrashlyticsLog.e(TAG, "Could not prepare json reading call object: " + e);
                disconnect();
                return;
            }

            while (socket.isConnected()) {
                try {
                    Message message = readJsonCall.call();
                    long received = System.currentTimeMillis();

                    CrashlyticsLog.d(TAG, "Received: " + message);

                    switch (message.getType()) {
                        case START_RECORD:
                            updateState(RecordingState.RECORDING);

                            StartRecord startRecord = (StartRecord) message;

                            try {
                                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                        Config.SAMPLE_RATE,
                                        Config.CHANNEL_CONFIG,
                                        Config.AUDIO_FORMAT,
                                        Config.BUFFER_SIZE);
                            } catch (Exception e) {
                                CrashlyticsLog.e(TAG, "Could not initialize AudioRecord", e);
                            }

                            if (mAudioRecord == null) {
                                CrashlyticsLog.e(TAG, "Could not initialize AudioRecord");
                            }

                            mRecordId = System.currentTimeMillis();

                            final StreamForwardTask streamForwardTask = new StreamForwardTask(
                                    new AudioRecordInputStream(mAudioRecord),
                                    socket.getOutputStream());

                            streamForwardTask.setListener(new StreamForwardTask.Listener() {
                                @Override
                                public void onStreamForwardFinished() {
                                    Intent broadcast = new Intent(Util.getFullName(ClientService.this, Action.TRANSFER_COMPLETED));
                                    broadcast.putExtra(Util.getFullName(ClientService.this, Extra.RECORD_ID), mRecordId);
                                    sendBroadcast(broadcast);
                                }

                                @Override
                                public void onStreamForwardError() {

                                }
                            });

                            Notification notification = getNotificationBuilder()
                                    .setContentText(getString(R.string.recording))
                                    .build();
                            startForeground(NOTIF_ID, notification);

                            mStreamForwardFuture = mExecutor.submit(streamForwardTask);
                            mScheduledExecutor.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    mAudioRecord.startRecording();

                                    CrashlyticsLog.d(TAG, "Started recording; id: " + mRecordId);

                                    Intent broadcast = new Intent(Util.getFullName(ClientService.this, Action.RECORDING_STARTED));
                                    broadcast.putExtra(Util.getFullName(ClientService.this, Extra.RECORD_ID), mRecordId);
                                    sendBroadcast(broadcast);
                                }
                            }, received - startRecord.time, TimeUnit.MILLISECONDS);
                            break;

                        case STOP_RECORD:
                            StopRecord stopRecord = (StopRecord) message;
                            mStreamForwardFuture.cancel(true);
                            mScheduledExecutor.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    mAudioRecord.stop();
                                    mAudioRecord.release();
                                    mAudioRecord = null;

                                    stopForeground(true);

                                    updateState(RecordingState.CONNECTED);

                                    CrashlyticsLog.d(TAG, "Stopped recording");
                                }
                            }, received - stopRecord.time, TimeUnit.MILLISECONDS);
                            break;
                    }
                } catch (IOException e) {
                    CrashlyticsLog.e(TAG, "Client session error: " + e);
                    updateState(RecordingState.DISCONNECTED);
                    stopSelf();
                    return;
                }
            }
        }
    }

    public enum RecordingState {
        DISCONNECTED,
        CONNECTED,
        RECORDING;
    }

    public enum MessageWhat {
        UPDATE_STATE;
    }
}