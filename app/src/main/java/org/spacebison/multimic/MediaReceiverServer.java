package org.spacebison.multimic;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import org.spacebison.multimic.audio.WavFileEncoder;
import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.OnConnectionErrorListener;
import org.spacebison.multimic.net.OnDisconnectedListener;
import org.spacebison.multimic.net.OnSocketBytesTransferredListener;
import org.spacebison.multimic.net.Protocol;
import org.spacebison.multimic.net.Server;
import org.spacebison.multimic.net.discovery.MulticastServiceProvider;
import org.spacebison.multimic.net.discovery.OnRequestReceivedListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cmb on 25.10.15.
 */
public class MediaReceiverServer {
    private static final String TAG = "cmb.MediaReceiver";
    private static final int BUFFER_SIZE = 10240;
    public static final int BUFFER_SIZE_IN_BYTES = 16 * AudioRecord.getMinBufferSize(44100, 1, AudioFormat.ENCODING_PCM_16BIT);

    private static MediaReceiverServer sInstance;
    private static final Object LOCK = new Object();

    private MulticastServiceProvider mServiceProvider =
            new MulticastServiceProvider(
                    Protocol.DISCOVERY_MULTICAST_GROUP,
                    Protocol.DISCOVERY_MULTICAST_PORT,
                    Protocol.SERVER_PORT);
    private Server mServer = new Server(Protocol.SERVER_PORT);
    private LinkedList<Socket> mClients = new LinkedList<>();
    private HashMap<Socket, ClientConnectionSession> mSessions = new HashMap<>();
    private boolean running = false;
    private OnConnectedListener mOnConnectedListener;
    private OnConnectionErrorListener mOnConnectionErrorListener;
    private OnDisconnectedListener mOnDisconnectedListener;
    private OnSocketBytesTransferredListener mOnSocketBytesTransferredListener;
    private ExecutorService mExecutor = Executors.newCachedThreadPool();
    private ExecutorService mUncertainExecutor = Util.newMostCurrentTaskExecutor();
    //private AudioRecord mAudioRecord;
    private AudioRecordSession mAudioRecordSession;

    public static MediaReceiverServer getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new MediaReceiverServer();
                }
            }
        }
        return sInstance;
    }

    private MediaReceiverServer() {
        mServer.setOnConnectedListener(new OnConnectedListener() {
            @Override
            public void onConnected(final Socket socket) {
                Log.d(TAG, "Connected: " + socket.getInetAddress());
                mClients.add(socket);
                Log.d(TAG, "" + mClients.size() + " clients connected");
                MediaReceiverServer.this.onConnected(socket);
            }
        });

        mServer.setOnConnectionErrorListener(new OnConnectionErrorListener() {
            @Override
            public void onConnectionError(final Socket socket, final Exception e) {
                Log.d(TAG, "Connection error: " + socket.getInetAddress() + ": " + e);
                mClients.remove(socket);
                Log.d(TAG, "" + mClients.size() + " clients connected");
                MediaReceiverServer.this.onConnectionError(socket, e);
            }
        });

        mServer.setOnDisconnectedListener(new OnDisconnectedListener() {
            @Override
            public void onDisconnected(final Socket socket) {
                Log.d(TAG, "Disconnected: " + socket.getInetAddress());
                mClients.remove(socket);
                Log.d(TAG, "" + mClients.size() + " clients connected");
                MediaReceiverServer.this.onDisconnected(socket);
            }
        });
    }

    private void onDisconnected(final Socket socket) {
        if (mOnDisconnectedListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnDisconnectedListener.onDisconnected(socket);
                }
            });
        }
    }


    private void onConnected(final Socket socket) {
        if (mOnConnectedListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnConnectedListener.onConnected(socket);
                }
            });
        }
    }

    private void onConnectionError(final Socket socket, final Exception e) {
        if (mOnConnectionErrorListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnConnectionErrorListener.onConnectionError(socket, e);
                }
            });
        }
    }

    private void onSocketBytesTransferred(final Socket socket, final int bytes) {
        if (mOnSocketBytesTransferredListener != null) {
            mUncertainExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mOnSocketBytesTransferredListener.onSocketBytesTransferred(socket, bytes);
                }
            });
        }
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        mServer.start();
        mServiceProvider.start();
        running = true;
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        mServer.disconnect();
        mServiceProvider.stop();
        running = false;
    }


    public void setOnRequestReceivedListener(OnRequestReceivedListener onRequestReceivedListener) {
        mServiceProvider.setOnRequestReceivedListener(onRequestReceivedListener);
    }

    public void startReceiving() {
        Log.d(TAG, "Start receiving");
        File sdCard = Environment.getExternalStorageDirectory();
        String dirPath = sdCard.getAbsolutePath() + "/multimic";
        final long now = System.currentTimeMillis();

        final File localFile = new File(dirPath + "/rec_" + now + "_0.raw");

        Log.d(TAG, "Dir: " + dirPath);
        Log.d(TAG, "Preparing to start " + mClients.size() + " sessions");
        File file = null;
        int i = 1;
        for (Socket s : mClients) {
            OutputStream os = null;
            try {
                String fileName = "/rec_" + now + '_' + i++ + ".raw";
                file = new File(dirPath);
                file.mkdirs();

                file = new File(dirPath + fileName);
                if (file.exists()) {
                    file.delete();
                }

                Log.d(TAG, "Using file " + file.getAbsolutePath() + " for " + s.getInetAddress());
                os = new FileOutputStream(file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error opening file to write: " + e);
                continue;
            }
            ClientConnectionSession session = new ClientConnectionSession(s, os);
            final File finalFile = file;
            session.setListener(new OnSessionEndedListener() {
                @Override
                public void onSessionEnded() {
                    WavFileEncoder.getInstance().encode(finalFile);
                }
            });
            mSessions.put(s, session);
            session.start();
        }

        mAudioRecordSession = new AudioRecordSession(localFile, new OnRecordingEndedListener() {
            @Override
            public void onRecordingEnded() {
                WavFileEncoder.getInstance().encode(localFile);
            }
        });
        mAudioRecordSession.start();
    }

    public void stopReceiving() {
        Log.d(TAG, "Stop receiving");
        mAudioRecordSession.interrupt();
        for (ClientConnectionSession s : mSessions.values()) {
            s.end();
        }
        mSessions.clear();
        mAudioRecordSession = null;
    }

    public List<InetAddress> getClientList() {
        ArrayList<InetAddress> list = new ArrayList<>(mClients.size());

        for(Socket c : mClients) {
            list.add(c.getInetAddress());
        }

        return list;
    }

    public void setOnConnectionErrorListener(OnConnectionErrorListener onConnectionErrorListener) {
        mOnConnectionErrorListener = onConnectionErrorListener;
    }

    public void setOnDisconnectedListener(OnDisconnectedListener onDisconnectedListener) {
        mOnDisconnectedListener = onDisconnectedListener;
    }

    public void setOnConnectedListener(OnConnectedListener onConnectedListener) {
        mOnConnectedListener = onConnectedListener;
    }

    public void setUncertainExecutor(ExecutorService uncertainExecutor) {
        mUncertainExecutor = uncertainExecutor;
    }

    public void setOnSocketBytesTransferredListener(OnSocketBytesTransferredListener onSocketBytesTransferredListener) {
        mOnSocketBytesTransferredListener = onSocketBytesTransferredListener;
    }

    private interface OnSessionEndedListener {
        void onSessionEnded();
    }

    private interface OnRecordingEndedListener {
        void onRecordingEnded();
    }

    private class AudioRecordSession extends Thread {
        private static final String TAG = "cmb.AudioRecordSession";
        private File mFile;
        private OnRecordingEndedListener mListener;

        public AudioRecordSession(File file, OnRecordingEndedListener listener) {
            mFile = file;
            mListener = listener;
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting local record session");
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    44100,
                    1,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE_IN_BYTES);

            if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Log.e(TAG, "Error initializing audioRecord");
                audioRecord.release();
                return;
            }
            Log.d(TAG, "Audio record buffer: " + BUFFER_SIZE_IN_BYTES);

            try {
                FileOutputStream out = new FileOutputStream(mFile);
                byte[] buf = new byte[BUFFER_SIZE_IN_BYTES];
                int bytesRead = 0;
                audioRecord.startRecording();
                while (!isInterrupted()) {
                    bytesRead = audioRecord.read(buf, 0, buf.length);
                    out.write(buf, 0, bytesRead);
                }

                audioRecord.stop();
                Log.d(TAG, "Flushing audio buffer to file");

                while((bytesRead = audioRecord.read(buf, 0, buf.length)) > 0) {
                    out.write(buf, 0, bytesRead);
                }

                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Releasing");

            audioRecord.release();

            Log.d(TAG, "Finished recording");

            if (mListener != null) {
                mListener.onRecordingEnded();
            }
        }
    }

    private class ClientConnectionSession extends Thread {
        private static final String TAG = "cmb.ClientConneSession";
        private Socket mSocket;
        private OutputStream mOutput;
        private OutputStream mClientOutput;
        private OnSessionEndedListener mListener;

        public ClientConnectionSession(Socket socket, OutputStream output) {
            mSocket = socket;
            mOutput = output;
            try {
                mClientOutput = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void end() {
            try {
                mClientOutput.write(Protocol.STOP_RECORD);
                mClientOutput.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            interrupt();
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting client session: " + mSocket.getInetAddress());
            InputStream input = null;
            try {
                byte[] buf = new byte[BUFFER_SIZE];
                input =  mSocket.getInputStream();

                //while (input.read(buf) > 0);

                mClientOutput.write(Protocol.START_RECORD);
                mClientOutput.flush();

                int byteSum = 0;
                int bytesRead = 0;
                while ((bytesRead = input.read(buf)) > 0) {
                    mOutput.write(buf, 0, bytesRead);
                    byteSum += bytesRead;
                    onSocketBytesTransferred(mSocket, byteSum);
                }
                mOutput.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error receiving from " + mSocket.getInetAddress() + ": " + e);
                mSessions.remove(mSocket);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }

                    try {
                        mOutput.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG, "Finished session " + mSocket.getInetAddress());
            mSessions.remove(mSocket);

            if (mSocket.isClosed()) {
                mClients.remove(mSocket);if (mOnDisconnectedListener != null) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mOnDisconnectedListener.onDisconnected(mSocket);
                        }
                    });
                }
                onDisconnected(mSocket);
            }

            if (mListener != null) {
                mListener.onSessionEnded();
            }
        }

        private void onDisconnected(final Socket socket) {
            if (mOnDisconnectedListener != null) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mOnDisconnectedListener.onDisconnected(mSocket);
                    }
                });
            }
        }

        public void setListener(OnSessionEndedListener listener) {
            mListener = listener;
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, final int offset, final int count) {
        char[] hexChars = new char[count * 2];
        final int end = offset + count;
        for ( int j = offset; j < end; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
