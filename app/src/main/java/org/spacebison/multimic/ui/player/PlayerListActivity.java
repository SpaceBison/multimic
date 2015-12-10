package org.spacebison.multimic.ui.player;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.spacebison.multimic.R;
import org.spacebison.multimic.Util;
import org.spacebison.multimic.model.MediaReceiverServer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class PlayerListActivity extends AppCompatActivity {
    private static final String TAG = "cmb.PlayerList";
    private SessionListAdapter mSessionListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_list);

        mSessionListAdapter = new SessionListAdapter();
        String recordingDirPath = MediaReceiverServer.getRecordingDirPath();
        File recordingDir = new File(recordingDirPath);
        String[] files = recordingDir.list();

        for (String file : files) {
            Log.d(TAG, "Adding " + file);
            mSessionListAdapter.add(file);
        }

        ListView listView = (ListView) findViewById(R.id.sessionList);
        listView.setAdapter(mSessionListAdapter);
    }

    private class SessionListAdapter extends BaseAdapter {
        private TreeMap<String, HashSet<Integer>> mSessions = new TreeMap<>();
        private boolean mReversed = false;

        public void add(String recording) {
            String noExtension = recording.substring(0, recording.lastIndexOf('.'));
            int lastUnderScore = noExtension.lastIndexOf('_');
            String sessionName = noExtension.substring(0, lastUnderScore);
            Integer recordingNumber = Integer.valueOf(noExtension.substring(lastUnderScore + 1, noExtension.length()));

            if (!mSessions.containsKey(sessionName)) {
                mSessions.put(sessionName, new HashSet<Integer>(recordingNumber));
            }

            mSessions.get(sessionName).add(recordingNumber);
        }

        @Override
        public int getCount() {
            return mSessions.size();
        }

        @Override
        public Object getItem(int position) {
            Set<String> keySet = isReversed() ? mSessions.navigableKeySet() : mSessions.descendingKeySet();
            return Util.getObjectAt(keySet, position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            String key = (String) getItem(position);
            int recordingCount = mSessions.get(key).size();

            text1.setText(key);
            text2.setText(getString(R.string.tracks) + ": " + recordingCount);

            return convertView;
        }

        public boolean isReversed() {
            return mReversed;
        }

        public void setReversed(boolean reversed) {
            mReversed = reversed;
        }
    }
}
