package org.spacebison.multimic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.spacebison.multimic.net.Protocol;
import org.spacebison.multimic.net.discovery.MulticastServiceResolver;
import org.spacebison.multimic.net.discovery.OnServiceResolvedListener;

import java.net.InetAddress;
import java.util.LinkedList;


public class ServerSearchActivity extends Activity {
    private static final String TAG = "cmb.ServerSearch";
    private ArrayAdapter mArrayAdapter;
    private LinkedList<ServerAddress> mServerList = new LinkedList<>();
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
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mServerList);
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
    }

    public void clickSearch(View view) {
        mServiceResolver.resolve(10000);
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
