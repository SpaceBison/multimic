package org.spacebison.multimic.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import org.spacebison.multimic.MediaSenderRecorder;
import org.spacebison.multimic.R;

import java.net.InetAddress;

/**
 * Created by cmb on 27.10.15.
 */
public class AudioRecordActivity extends Activity {
    private static final String TAG = "cmb.AudioRecordA";
    public static final String EXTRA_SERVER_ADDRESS = "serverAddress";
    public static final String EXTRA_SERVER_PORT = "serverPort";
    private MediaSenderRecorder mMediaSenderRecorder = MediaSenderRecorder.getInstance();;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            InetAddress address = (InetAddress) extras.getSerializable(EXTRA_SERVER_ADDRESS);
            int port = extras.getInt(EXTRA_SERVER_PORT);
            mMediaSenderRecorder.connect(address, port);
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit?");
        builder.setMessage("Exit the server?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaSenderRecorder.release();
    }
}
