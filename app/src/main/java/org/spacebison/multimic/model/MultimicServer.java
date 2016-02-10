package org.spacebison.multimic.model;

import org.spacebison.multimic.net.ListeningServer;
import org.spacebison.multimic.net.message.NtpRequest;
import org.spacebison.multimic.net.message.NtpResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cmb on 10.02.16.
 */
public class MultimicServer implements ListeningServer.Listener {
    private final String TAG = "MultimicServer";
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final ListeningServer mServer = new ListeningServer(Config.SERVER_PORT);
    private final LinkedList<Client> mClients = new LinkedList<>();

    public MultimicServer() {
        mServer.setListener(this);
    }

    @Override
    public void onClientConnected(Socket socket) {

    }

    @Override
    public void onListeningError(Exception e) {

    }

    private class GetTimeOffsetTask implements Runnable {
        private Socket mSocket;

        public GetTimeOffsetTask(Socket socket) {
            mSocket = socket;
        }

        @Override
        public void run() {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(mSocket.getInputStream());
                        NtpResponse ntpResponse = (NtpResponse) ois.readObject();
                        long ntpResponseReceiveTime = System.currentTimeMillis();

                        synchronized (mClients) {
                            mClients.add(new Client(mSocket,
                                            ntpResponse.requestSendTime,
                                            ntpResponse.requestReceiveTime,
                                            ntpResponse.responseSendTime,
                                            ntpResponseReceiveTime));
                        }

                        // TODO: 10.02.16 call a callback or launch client handling task
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });

            try {
                ObjectOutputStream oos = new ObjectOutputStream(mSocket.getOutputStream());
                oos.writeObject(new NtpRequest(System.currentTimeMillis()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Client {
        public Socket socket;
        public long timeOffset;
        public long delay;

        public Client(Socket clientSocket, long ntpRequestSendTime, long ntpRequestReceiveTime, long ntpResponseSendTime, long ntpResponseReceiveTime) {
            socket = clientSocket;
            delay = ((ntpResponseReceiveTime - ntpRequestSendTime) - (ntpResponseSendTime - ntpRequestReceiveTime));
            timeOffset = ((ntpRequestReceiveTime - ntpRequestSendTime) + (ntpResponseSendTime - ntpRequestReceiveTime));
        }
    }
}
