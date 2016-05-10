package org.spacebison.multimic.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;

import org.spacebison.common.CrashlyticsLog;
import org.spacebison.common.Util;
import org.spacebison.multimic.R;
import org.spacebison.multimic.model.ClientService;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ClientActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "ClientActivity";
    private final Receiver mReceiver = new Receiver();
    @Bind(R.id.image)
    protected ImageView mImage;
    @Bind(R.id.toolbar)
    protected Toolbar mToolbar;
    private Messenger mClientService;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mClientService = new Messenger(service);
        try {
            ClientService.requestStateUpdate(mClientService);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mClientService = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        registerReceiver(mReceiver, ClientService.getIntentFilter(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mClientService != null) {
            unbindService(this);
            mClientService = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, ClientService.class), this, 0);
    }

    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            CrashlyticsLog.d(TAG, "Received: " + intent);
            if (intent.getAction().equals(Util.getFullName(context, ClientService.Action.STATE_CHANGED))) {
                final ClientService.RecordingState state = (ClientService.RecordingState) intent.getSerializableExtra(Util.getFullName(context, ClientService.Extra.STATE));
                switch (state) {
                    case DISCONNECTED:
                    case CONNECTED:
                        mImage.setImageResource(R.drawable.ic_mic_none_big);
                        break;
                    case RECORDING:
                        mImage.setImageResource(R.drawable.ic_mic_big);
                        break;
                }

                final ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(state.name());
                }
            }
        }
    }
}
