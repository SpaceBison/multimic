package org.spacebison.multimic.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.model.Protocol;
import org.spacebison.multimic.net.discovery.MulticastServiceResolver;

import java.net.InetAddress;
import java.util.LinkedList;


public class ServerSearchActivity extends AppCompatActivity {
    private static final String TAG = "cmb.ServerSearch";
    private ArrayAdapter mArrayAdapter;
    private LinkedList<ServerAddress> mServerList = new LinkedList<>();
    private Tracker mTracker;
    private MulticastServiceResolver mServiceResolver =
            new MulticastServiceResolver(
                    Protocol.DISCOVERY_MULTICAST_GROUP,
                    Protocol.DISCOVERY_MULTICAST_PORT,
                    new OnServiceResolvedListener() {
        @Override
        public void onServiceResolved(InetAddress address, int port) {
            mServerList.add(new ServerAddress(address, port));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mArrayAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onResolveEnded() {
            Log.d(TAG, "Resolve ended");
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_search);
        ListView serverListView = (ListView) findViewById(R.id.serverList);
        mArrayAdapter = new ArrayAdapter<>(this, R.layout.list_item, mServerList);
        serverListView.setAdapter(mArrayAdapter);
        serverListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ServerAddress serverAddress = (ServerAddress) adapterView.getAdapter().getItem(i);
                Intent intent = new Intent(ServerSearchActivity.this, AudioRecordActivity.class);
                intent.putExtra(AudioRecordActivity.EXTRA_SERVER_ADDRESS, serverAddress.mAddress);
                intent.putExtra(AudioRecordActivity.EXTRA_SERVER_PORT, serverAddress.mPort);
                startActivity(intent);
            }
        });

        final Button searchButton = (Button) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceResolver.resolve(10000);
                searchButton.setEnabled(false);
                searchButton.setText(R.string.searching);
                searchButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        searchButton.setEnabled(true);
                        searchButton.setText(R.string.search);
                    }
                }, 10000);
            }
        });

        mTracker = MultimicApplication.getDefaultTracker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("ListeningServer Search");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private class ServerAddress {
        private InetAddress mAddress;
        private int mPort;

        public ServerAddress(InetAddress address, int port) {
            mAddress = address;
            mPort = port;
        }

        @Override
        public String toString() {
            return mAddress.toString() + ':' + mPort;
        }
    }
}
