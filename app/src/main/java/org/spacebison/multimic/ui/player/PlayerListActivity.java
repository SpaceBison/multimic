package org.spacebison.multimic.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.Util;
import org.spacebison.multimic.model.MediaReceiverServer;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class PlayerListActivity extends AppCompatActivity {
    private static final String TAG = "cmb.PlayerList";
    private SessionListAdapter mSessionListAdapter;
    private Tracker mTracker;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_list);

        mSessionListAdapter = new SessionListAdapter();
        String recordingDirPath = MediaReceiverServer.getRecordingDirPath();
        File recordingDir = new File(recordingDirPath);
        File[] files = recordingDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        for (File file : files) {
            mSessionListAdapter.add(file.getName());
        }

        ListView listView = (ListView) findViewById(R.id.sessionList);
        listView.setAdapter(mSessionListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String sessionPrefix = (String) mSessionListAdapter.getItem(position);
                Intent intent = new Intent(PlayerListActivity.this, PlayerActivity.class);
                intent.putExtra(PlayerActivity.EXTRA_SESSION_PREFIX, sessionPrefix);
                startActivity(intent);
            }
        });

        mTracker = MultimicApplication.getDefaultTracker();
    }

    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Session List");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private class SessionListAdapter extends BaseAdapter {
        private TreeMap<String, HashSet<Integer>> mSessions = new TreeMap<>();
        private boolean mReversed = false;

        public void add(String recording) {
            Log.d(TAG, "Adding " + recording);

            if (!recording.contains(".")) {
                return;
            }

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
