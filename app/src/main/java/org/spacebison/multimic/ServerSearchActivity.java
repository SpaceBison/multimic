package org.spacebison.multimic;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
