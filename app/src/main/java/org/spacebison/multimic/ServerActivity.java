package org.spacebison.multimic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.discovery.OnRequestReceivedListener;

import java.net.DatagramPacket;
import java.net.Socket;

/**
 * Created by cmb on 25.10.15.
 */
public class ServerActivity extends Activity implements OnConnectedListener, OnRequestReceivedListener {
    private static final String TAG = "cmb.ServerActivity";
    private MediaReceiverServer mServer = new MediaReceiverServer();

    public void clickStart(View view) {
        mServer.startReceiving();
    }

    public void clickStop(View view) {
        mServer.stopReceiving();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        mServer.setOnConnectedListener(this);
    }

    @Override
    public void onConnected(Socket socket) {
        Log.d(TAG, "Client connected: " + socket.getInetAddress());
    }

    @Override
    public void onRequestReceived(DatagramPacket packet) {
        Log.d(TAG, "Received request from: " + packet.getAddress());
    }

    public void clickStartServer(View view) {
        Log.d(TAG, "Starting server");
        mServer.start();
    }

    public void clickLogs(View view) {
        startActivity(new Intent(this, LogActivity.class));
    }
}
