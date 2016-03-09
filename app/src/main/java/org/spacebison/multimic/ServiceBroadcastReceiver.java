package org.spacebison.multimic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by cmb on 05.03.16.
 */
public abstract class ServiceBroadcastReceiver extends BroadcastReceiver {
    public void register(Context context) {
        context.registerReceiver(this, MultimicService.getIntentFilter());
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case MultimicService.ACTION_CONNECTED:
                onConnected(context, intent.getStringExtra(MultimicService.EXTRA_SERVER_NAME));
                break;

            case MultimicService.ACTION_DISCONNECTED:
                onDisconnected(context, intent.getStringExtra(MultimicService.EXTRA_SERVER_NAME));
                break;

            case MultimicService.ACTION_CLIENT_CONNECTED:
                onClientConnected(context, intent.getStringExtra(MultimicService.EXTRA_CLIENT_NAME));
                break;

            case MultimicService.ACTION_CLIENT_DISCONNECTED:
                onClientDisconnected(context, intent.getStringExtra(MultimicService.EXTRA_CLIENT_NAME));
                break;
        }
    }

    public abstract void onConnected(Context context, String serverName);
    public abstract void onDisconnected(Context context, String serverName);
    public abstract void onClientConnected(Context context, String clientName);
    public abstract void onClientDisconnected(Context context, String clientName);
}
