package org.spacebison.multimic.net;

import org.spacebison.common.CrashlyticsLog;

import org.spacebison.multimic.net.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * Created by cmb on 11.02.16.
 */
public class ReadMessageCall<T extends Message> implements Callable<T> {
    private static final String TAG = "ReadMessageCall";
    private Socket mSocket;

    public ReadMessageCall(Socket socket) {
        mSocket = socket;
    }

    @Override
    public T call() throws Exception {
        try {
            ObjectInputStream ois = new ObjectInputStream(mSocket.getInputStream());
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            CrashlyticsLog.e(TAG, "NTP Error: " + e);
            return null;
        }
    }
}
