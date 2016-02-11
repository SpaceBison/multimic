package org.spacebison.multimic.ui.server;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.spacebison.multimic.R;
import org.spacebison.multimic.model.MediaReceiverServer;

import java.net.Socket;

/**
 * Created by cmb on 04.11.15.
 */
public class ClientListFragment extends Fragment implements OnConnectedListener {
    private ListView mListView;
    private ArrayAdapter<Socket> mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_client_list, container, false);

        mListView = (ListView) v.findViewById(R.id.list);
        mAdapter = new ArrayAdapter<Socket>(getContext(), android.R.layout.simple_list_item_1);
        mAdapter.addAll(MediaReceiverServer.getInstance().getClientList());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaReceiverServer.getInstance().sendNtpRequest(mAdapter.getItem(position));
            }
        });
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
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateList();
                }
            });
        }
    }

    public void updateList() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.clear();
                    mAdapter.addAll(MediaReceiverServer.getInstance().getClientList());
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }
}
