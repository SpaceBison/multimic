package org.spacebison.multimic.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;
import android.view.View;

import org.spacebison.multimic.MultimicService;
import org.spacebison.multimic.R;
import org.spacebison.multimic.ToastServiceBroadcastReceiver;

public class MainActivity extends Activity implements ServiceConnection {
    private static final String TAG = "MainActivity";
    private final ToastServiceBroadcastReceiver mServiceListener = new ToastServiceBroadcastReceiver();
    private Messenger mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mServiceListener.register(this);

        Log.d(TAG, "Bind service");
        if (!bindService(new Intent(this, MultimicService.class), this, BIND_AUTO_CREATE)) {
            Log.e(TAG, "Did not bind");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        mServiceListener.unregister(this);
    }

    public void onServerClick(View view) {
        startActivity(new Intent(this, ServerActivity.class));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "Service connected: " + name);
        mService = new Messenger(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    public void onServerSearchClick(View view) {
        startActivity(new Intent(this, ServerSearchActivity.class));
    }
}
