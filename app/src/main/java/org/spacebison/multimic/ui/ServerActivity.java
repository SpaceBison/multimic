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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.spacebison.common.CrashlyticsLog;
import org.spacebison.common.Util;
import org.spacebison.multimic.R;
import org.spacebison.multimic.model.ClientService;
import org.spacebison.multimic.model.ServerService;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by cmb on 08.03.16.
 */
public class ServerActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "ServerActivity";

    private final Adapter mAdapter = new Adapter();
    private final Receiver mReceiver = new Receiver();

    @Bind(R.id.toolbar)
    protected Toolbar mToolbar;
    @Bind(R.id.recycler)
    protected RecyclerView mRecyclerView;
    @Bind(R.id.image)
    protected ImageView mImage;
    @Bind(R.id.fab)
    protected FloatingActionButton mFab;

    private Messenger mServerService;

    private boolean record = true;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        CrashlyticsLog.d(TAG, "Service connected: " + name);
        mServerService = new Messenger(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        CrashlyticsLog.d(TAG, "Service disconnected: " + name);
        mServerService = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.fab)
    public void recordButtonClick(View v) {
        CrashlyticsLog.v(TAG, "Record click");

        if (mServerService == null) {
            CrashlyticsLog.w(TAG, "No service");
            return;
        }

        try {
            if (record) {
                ServerService.startRecording(mServerService);
            } else {
                ServerService.stopRecording(mServerService);
            }
        } catch (RemoteException e) {
            CrashlyticsLog.e(TAG, "Error communicating with service", e);
        }

        record = !record;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        registerReceiver(mReceiver, ClientService.getIntentFilter(this));
        registerReceiver(mReceiver, ServerService.getIntentFilter(this));

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        bindService(new Intent(this, ServerService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServerService != null) {
            unbindService(this);
            mServerService = null;
        }

        unregisterReceiver(mReceiver);
    }

    protected class Holder extends RecyclerView.ViewHolder {
        @Bind(R.id.name)
        TextView mName;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        private final LinkedList<String> mClients = new LinkedList<>();

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new Holder(inflater.inflate(R.layout.item_client, parent, false));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.mName.setText(mClients.get(position));
        }

        @Override
        public int getItemCount() {
            return mClients.size();
        }

        public void add(int location, String object) {
            mClients.add(location, object);
        }

        public boolean addAll(int location, Collection<? extends String> collection) {
            return mClients.addAll(location, collection);
        }

        public boolean addAll(Collection<? extends String> collection) {
            return mClients.addAll(collection);
        }

        public void clear() {
            mClients.clear();
        }

        public String remove() {
            return mClients.remove();
        }
    }

    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            CrashlyticsLog.d(TAG, "Received: " + intent);
            if (intent.getAction().equals(Util.getFullName(context, ServerService.Action.CLIENT_LIST_CHANGED))) {
                mAdapter.clear();
                final List<String> names = Arrays.asList(intent.getStringArrayExtra(Util.getFullName(context, ServerService.Extra.CLIENT_ARRAY)));
                Log.d(TAG, "Names: " + names);
                mAdapter.addAll(names);
                mAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals(Util.getFullName(context, ClientService.Action.STATE_CHANGED))) {
                ClientService.RecordingState state = (ClientService.RecordingState) intent.getSerializableExtra(Util.getFullName(context, ClientService.Extra.STATE));
                switch (state) {
                    case DISCONNECTED:
                    case CONNECTED:
                        mImage.setImageResource(R.drawable.ic_mic_none_big);
                        mFab.setImageResource(R.drawable.ic_mic_24dp_white);
                        break;
                    case RECORDING:
                        mImage.setImageResource(R.drawable.ic_mic_big);
                        mFab.setImageResource(R.drawable.ic_stop_24dp);
                        break;
                }
            }
        }
    }
}
