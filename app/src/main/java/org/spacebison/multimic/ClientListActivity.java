package org.spacebison.multimic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.spacebison.multimic.net.OnConnectedListener;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by cmb on 04.11.15.
 */
public class ClientListActivity extends AppCompatActivity implements OnConnectedListener {
    ListView mListView;
    ArrayAdapter<InetAddress> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_list);

        mListView = (ListView) findViewById(R.id.list);
        mAdapter = new ArrayAdapter<InetAddress>(this, android.R.layout.simple_list_item_1);
        mAdapter.addAll(MediaReceiverServer.getInstance().getClientList());
        mListView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MediaReceiverServer.getInstance().setOnConnectedListener(this);
        updateList();
    }

    @Override
    public void onConnected(Socket socket) {
        updateList();
    }

    public void updateList() {
        mAdapter.clear();
        mAdapter.addAll(MediaReceiverServer.getInstance().getClientList());
        mAdapter.notifyDataSetChanged();
    }
}
