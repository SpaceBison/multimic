package org.spacebison.multimic.model;

import android.util.Log;

import org.spacebison.multimic.net.ListeningServer;
import org.spacebison.multimic.net.ReadMessageCall;
import org.spacebison.multimic.net.ReadNtpResponseCall;
import org.spacebison.multimic.net.message.Hello;
import org.spacebison.multimic.net.message.NtpRequest;
import org.spacebison.multimic.net.message.NtpResponse;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by cmb on 10.02.16.
 */
public class MultimicServer implements ListeningServer.Listener {
    private static final String TAG = "MultimicServer";
    private static final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final ListeningServer mServer = new ListeningServer(Config.SERVER_PORT);
    private final LinkedList<Client> mClients = new LinkedList<>();

    private Listener mListener;

    public MultimicServer() {
        mServer.setListener(this);
    }

    @Override
    public void onClientConnected(final Socket socket) {
        Future<Hello> messageFuture = mExecutor.submit(new ReadMessageCall<Hello>(socket));

        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new NtpRequest(System.currentTimeMillis()));
        } catch (IOException e) {
            Log.e(TAG, "Error sending NTP request: " + e);
        }

        String name = null;
        try {
            Hello hello = messageFuture.get();
            if (hello != null) {
                name = hello.name;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Future<NtpResponse> ntpResponseFuture = mExecutor.submit(new ReadNtpResponseCall(socket));

        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new NtpRequest(System.currentTimeMillis()));
        } catch (IOException e) {
            Log.e(TAG, "Error sending NTP request: " + e);
        }

        long offset = 0;
        long delay = 0;
        try {
            NtpResponse ntpResponse = ntpResponseFuture.get();
            if (ntpResponse != null) {
                offset = ntpResponse.getOffset();
                delay = ntpResponse.getDelay();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Client newClient = new Client(socket, name, offset, delay);

        synchronized (mClients) {
            mClients.add(newClient);
        }
    }

    public LinkedList<Client> getClients() {
        return mClients;
    }

    @Override
    public void onListeningError(Exception e) {
        Log.e(TAG, "Listening error: " + e);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        void onClientConnected(Client client);
    }
}
