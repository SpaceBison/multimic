package org.spacebison.multimic.ui.server;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.spacebison.multimic.MediaReceiverServer;
import org.spacebison.multimic.R;

/**
 * Created by cmb on 02.12.15.
 */
public class ServerFragment extends Fragment {
    private boolean mRecording = false;
    private MediaReceiverServer mServer = MediaReceiverServer.getInstance();
    private Button mRecordButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_server, container, false);

        mRecordButton = (Button) v.findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRecord();
            }
        });
        return v;
    }


    public void clickRecord() {
        if (mRecording) {
            mServer.stopReceiving();
            mRecordButton.setText(R.string.record);
        } else {
            mServer.startReceiving();
            mRecordButton.setText(R.string.recording);
        }
        mRecording = !mRecording;
    }
}
