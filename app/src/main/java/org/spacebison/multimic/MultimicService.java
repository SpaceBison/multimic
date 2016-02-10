package org.spacebison.multimic;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.spacebison.common.ServiceBinder;
import org.spacebison.multimic.net.ListeningServer;
import org.spacebison.multimic.model.Protocol;

import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by cmb on 07.02.16.
 */
public class MultimicService extends Service implements ListeningServer.Listener {
    private static final String TAG = "MutlimicService";
    private final ServiceBinder<MultimicService> mBinder = new ServiceBinder<>(this);


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onClientConnected(Socket socket) {
        synchronized (mClients) {
            mClients.add(socket);
        }
    }

    @Override
    public void onListeningError(Exception e) {
        Log.e(TAG, "Listening error: " + e);
    }
}
