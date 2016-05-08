package org.spacebison.multimic;

import android.content.Context;
import android.widget.Toast;

import org.spacebison.common.CrashlyticsLog;

/**
 * Created by cmb on 06.03.16.
 */
public class ToastServiceBroadcastReceiver extends ServiceBroadcastReceiver {
    private static final String TAG = "ToastServiceBR";

    @Override
    public void onConnected(Context context, String serverName) {
        final String text = "Connected to " + serverName;
        showToast(context, text);
    }

    @Override
    public void onDisconnected(Context context, String serverName) {
        final String text = "Disconnected from " + serverName;
        showToast(context, text);
    }

    @Override
    public void onClientConnected(Context context, String clientName) {
        final String text = "Client connected: " + clientName;
        showToast(context, text);
    }

    @Override
    public void onClientDisconnected(Context context, String clientName) {
        final String text = "Client disconnected: " + clientName;
        showToast(context, text);
    }

    private void showToast(Context context, String text) {
        CrashlyticsLog.d(TAG, text);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
