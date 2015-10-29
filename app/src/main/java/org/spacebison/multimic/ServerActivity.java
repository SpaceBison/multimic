package org.spacebison.multimic;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import org.spacebison.multimic.audio.MediaReceiverServer;
import org.spacebison.multimic.net.OnConnectedListener;

import java.net.Socket;

/**
 * Created by cmb on 25.10.15.
 */
public class ServerActivity extends Activity implements OnConnectedListener {
    private static final String TAG = "cmb.ServerActivity";
    private MediaReceiverServer mServer = new MediaReceiverServer();

    private LogView mLog;

    @Override
    public void onConnected(Socket socket) {
        mLog.d(TAG, "Client connected: " + socket);
    }

    public void clickStart(View view) {
        mServer.start();
    }

    public void clickStop(View view) {
        mServer.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mLog = (LogView) findViewById(R.id.log);

        mServer.setOnConnectedListener(this);
    }
}
