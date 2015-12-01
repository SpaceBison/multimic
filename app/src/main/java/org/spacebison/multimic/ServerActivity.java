package org.spacebison.multimic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

/**
 * Created by cmb on 25.10.15.
 */
public class ServerActivity extends AppCompatActivity {
    private static final String TAG = "cmb.ServerActivity";
    private boolean mRecording = false;
    private MediaReceiverServer mServer;
    private Button mRecordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        mRecordButton = (Button) findViewById(R.id.recordButton);

        mServer = MediaReceiverServer.getInstance();
        mServer.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.server_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showClients:
                startActivity(new Intent(this, ClientListActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void clickRecord(View view) {
        if (mRecording) {
            mServer.stopReceiving();
            mRecordButton.setText("RECORD");
        } else {
            mServer.startReceiving();
            mRecordButton.setText("RECORDING");
        }
        mRecording = !mRecording;
    }
}
