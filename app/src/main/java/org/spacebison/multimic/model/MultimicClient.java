package org.spacebison.multimic.model;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

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
public class MultimicClient {
    private static final String TAG = "MultimicClient";
    private static final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private static final ScheduledExecutorService mScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final String mName;
    private Server mServer;
    private Listener mListener;
    private AudioRecord mAudioRecord;
    private Future mStreamForwardFuture;
    private long mRecordId;

    public MultimicClient(String name) {
        mName = name;
    }

    public static void resolveServers(int timeout, MulticastServiceResolver.Listener listener) {
        new MulticastServiceResolver(Config.DISCOVERY_MULTICAST_GROUP, Config.DISCOVERY_MULTICAST_PORT, listener)
                .resolve(timeout);
    }

    public void connect(final ResolvedService serverService) {
        mExecutor.execute(new ClientTask(serverService));
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void release() {
        releaseAudio();

    }

    public void disconnect() {
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

    public interface Listener {
        void onConnected(Server server);
        void onRecordingStarted(long id);
        void onAudioTransferComplete(long id);
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

                Log.d(TAG, "Waiting for a hello from server");
                Hello hello = new ReadJsonCall<>(socket.getInputStream(), Hello.class).call();

                Log.d(TAG, "Saying hello");
                new WriteJsonTask(socket.getOutputStream(), new Hello(mName)).run();

                Log.d(TAG, "Awaiting NTP request");
                NtpRequest ntpRequest = new ReadNtpRequestCall(socket.getInputStream()).call();

                Log.d(TAG, "Sending NTP response");
                new WriteJsonTask(socket.getOutputStream(), new NtpResponse(ntpRequest, System.currentTimeMillis())).run();

                Log.d(TAG, "Handshake complete");

                mServer = new Server(socket, hello.name);

                if (mListener != null) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onConnected(mServer);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to server: " + e);
                disconnect();
                return;
            }

            ReadJsonCall<Message> readJsonCall;
            try {
                readJsonCall = new ReadJsonCall<>(socket.getInputStream(), Message.class);
            } catch (IOException e) {
                Log.e(TAG, "Could not prepare json reading call object: " + e);
                disconnect();
                return;
            }

            while (socket.isConnected()) {
                try {
                    Message message = readJsonCall.call();
                    long received = System.currentTimeMillis();

                    Log.d(TAG, "Received: " + message);

                    switch (message.getType()) {
                        case START_RECORD:
                            StartRecord startRecord = (StartRecord) message;
                            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                    44100,
                                    1,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    Config.BUFFER_SIZE);

                            mRecordId = System.currentTimeMillis();

                            final StreamForwardTask streamForwardTask = new StreamForwardTask(
                                    new AudioRecordInputStream(mAudioRecord),
                                    socket.getOutputStream());

                            streamForwardTask.setListener(new StreamForwardTask.Listener() {
                                @Override
                                public void onStreamForwardFinished() {
                                    mExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mListener != null) {
                                                mListener.onAudioTransferComplete(mRecordId);
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onStreamForwardError() {

                                }
                            });

                            mStreamForwardFuture = mExecutor.submit(streamForwardTask);
                            mScheduledExecutor.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    mAudioRecord.startRecording();

                                    Log.d(TAG, "Started recording; id: " + mRecordId);

                                    if (mListener != null) {
                                        mListener.onRecordingStarted(mRecordId);
                                    }
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
                                    Log.d(TAG, "Stopped recording");
                                }
                            }, received - stopRecord.time, TimeUnit.MILLISECONDS);
                            break;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Client session error: " + e);
                    disconnect();
                    return;
                }
            }
        }
    }
}
