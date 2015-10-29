package org.spacebison.multimic;

import android.app.Activity;
import android.os.Bundle;

import org.spacebison.multimic.net.Client;

import java.net.InetAddress;

/**
 * Created by cmb on 27.10.15.
 */
public class AudioRecordActivity extends Activity {
    public static final String EXTRA_SERVER_ADDRESS = "serverAddress";
    public static final String EXTRA_SERVER_PORT = "serverPort";
    private Client mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        InetAddress address = (InetAddress) extras.getSerializable(EXTRA_SERVER_ADDRESS);
        int port = extras.getInt(EXTRA_SERVER_PORT);
        mClient = new Client(address, port);
        mClient.start();
    }
}
