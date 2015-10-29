package org.spacebison.multimic;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import org.spacebison.multimic.audio.MediaReceiverServer;
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

    private LogView mLog;

    public void clickStart(View view) {
        mLog.d(TAG, "Starting server");
        mServer.start();
    }

    public void clickStop(View view) {
        mLog.d(TAG, "Stopping server");
        mServer.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mLog = (LogView) findViewById(R.id.log);

        mServer.setOnConnectedListener(this);
    }

    @Override
    public void onConnected(Socket socket) {
        mLog.d(TAG, "Client connected: " + socket.getInetAddress());
    }

    @Override
    public void onRequestReceived(DatagramPacket packet) {
        mLog.d(TAG, "Received request from: " + packet.getAddress());
    }
}
