package org.spacebison.multimic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.spacebison.common.Util;
import org.spacebison.multimic.model.ClientService;
import org.spacebison.multimic.model.ServerService;

/**
 * Created by cmb on 05.03.16.
 */
public abstract class ServiceBroadcastReceiver extends BroadcastReceiver {
    public void register(Context context) {
        context.registerReceiver(this, ServerService.getIntentFilter(context));
        context.registerReceiver(this, ClientService.getIntentFilter(context));
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (receiveClient(context, intent)) {
            return;
        }

        receiveServer(context, intent);
    }

    private boolean receiveServer(Context context, Intent intent) {
        ServerService.Action serviceAction;
        try {
            serviceAction = ServerService.Action.valueOf(intent.getAction());
        } catch (IllegalArgumentException e) {
            return false;
        }

        switch (serviceAction) {
            case CLIENT_CONNECTED:
                onClientConnected(context, intent.getStringExtra(Util.getFullName(context, ServerService.Extra.CLIENT)));
                break;

            case CLIENT_DISCONNECTED:
                onClientDisconnected(context, intent.getStringExtra(Util.getFullName(context, ServerService.Extra.CLIENT)));
                break;

            default:
                return false;
        }

        return true;
    }

    private boolean receiveClient(Context context, Intent intent) {
        ClientService.Action serviceAction;
        try {
            serviceAction = ClientService.Action.valueOf(intent.getAction());
        } catch (IllegalArgumentException e) {
            return false;
        }

        switch (serviceAction) {
            case CONNECTED:
                onConnected(context, intent.getStringExtra(Util.getFullName(context, ClientService.Extra.SERVER)));
                break;

            case RECORDING_STARTED:
                break;

            case TRANSFER_COMPLETED:
                break;

            default:
                return false;
        }

        return true;
    }

    public abstract void onConnected(Context context, String serverName);
    public abstract void onDisconnected(Context context, String serverName);
    public abstract void onClientConnected(Context context, String clientName);
    public abstract void onClientDisconnected(Context context, String clientName);
}
