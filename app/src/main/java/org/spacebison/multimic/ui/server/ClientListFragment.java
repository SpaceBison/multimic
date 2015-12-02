package org.spacebison.multimic.ui.server;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.spacebison.multimic.MediaReceiverServer;
import org.spacebison.multimic.R;
import org.spacebison.multimic.net.OnConnectedListener;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by cmb on 04.11.15.
 */
public class ClientListFragment extends Fragment implements OnConnectedListener {
    ListView mListView;
    ArrayAdapter<InetAddress> mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_client_list, container, false);

        mListView = (ListView) v.findViewById(R.id.list);
        mAdapter = new ArrayAdapter<InetAddress>(getContext(), android.R.layout.simple_list_item_1);
        mAdapter.addAll(MediaReceiverServer.getInstance().getClientList());
        mListView.setAdapter(mAdapter);
        return v;
    }


    @Override
    public void onResume() {
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
