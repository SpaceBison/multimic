package org.spacebison.multimic.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.spacebison.multimic.MultimicService;
import org.spacebison.multimic.R;
import org.spacebison.multimic.ServiceBroadcastReceiver;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 08.03.16.
 */
public class ServerControlFragment extends Fragment implements ServiceConnection {
    private static final String TAG = "ServerControlFragment";

    @Bind(R.id.start_button)
    Button mStartButton;
    @Bind(R.id.stop_button)
    Button mStopButton;

    private final ServiceReceiver mServiceReceiver = new ServiceReceiver();
    private Messenger mService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_server_control, container, false);
        ButterKnife.bind(this, v);

        mStartButton.setEnabled(false);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mService.send(MultimicService.getMessage(MultimicService.MessageCode.START_RECORDING));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mStopButton.setEnabled(false);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mService.send(MultimicService.getMessage(MultimicService.MessageCode.STOP_RECORDING));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        return v;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "attach");
        super.onAttach(context);
        mServiceReceiver.register(context);
        if (!context.bindService(new Intent(getContext(), MultimicService.class), this, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Did not bind");
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "detach");
        super.onDetach();
        getContext().unbindService(this);
        mServiceReceiver.unregister(getContext());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "Service connected");
        mService = new Messenger(service);
        try {
            mService.send(MultimicService.getMessage(MultimicService.MessageCode.START_SERVER));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "Service disconnected");
        mStartButton.setEnabled(false);
        mStopButton.setEnabled(false);
    }

    private class ServiceReceiver extends ServiceBroadcastReceiver {
        @Override
        public void onConnected(Context context, String serverName) {
            Log.d(TAG, "Connected local: " + serverName);
            mStartButton.post(new Runnable() {
                @Override
                public void run() {
                    mStartButton.setEnabled(true);
                    mStopButton.setEnabled(true);
                }
            });
        }

        @Override
        public void onDisconnected(Context context, String serverName) {

        }

        @Override
        public void onClientConnected(Context context, String clientName) {

        }

        @Override
        public void onClientDisconnected(Context context, String clientName) {

        }
    }
}
