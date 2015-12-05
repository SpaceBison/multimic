package org.spacebison.multimic.ui.server;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.spacebison.multimic.model.MediaReceiverServer;
import org.spacebison.multimic.R;

/**
 * Created by cmb on 02.12.15.
 */
public class ServerFragment extends Fragment {
    private static final String STATE_RECORDING = "recording";

    private boolean mRecording = false;
    private MediaReceiverServer mServer = MediaReceiverServer.getInstance();
    private Button mRecordButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_server, container, false);

        if (savedInstanceState != null) {
            mRecording = savedInstanceState.getBoolean(STATE_RECORDING);
        }

        mRecordButton = (Button) v.findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRecord();
            }
        });

        setButtonText();

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RECORDING, mRecording);
    }

    public void clickRecord() {
        if (mRecording) {
            mServer.stopRecording();
        } else {
            mServer.startRecording();
        }
        mRecording = !mRecording;
        setButtonText();
    }

    private void setButtonText() {
        mRecordButton.setText(mRecording ? R.string.recording : R.string.record);
    }
}
