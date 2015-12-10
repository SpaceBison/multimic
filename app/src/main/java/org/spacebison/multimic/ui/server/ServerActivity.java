package org.spacebison.multimic.ui.server;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.Analytics;
import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.model.MediaReceiverServer;
import org.spacebison.multimic.model.RecordListener;
import org.spacebison.multimic.net.OnConnectedListener;
import org.spacebison.multimic.net.OnDisconnectedListener;

import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by cmb on 25.10.15.
 */
public class ServerActivity extends AppCompatActivity {
    private static final String TAG = "cmb.ServerActivity";
    private MediaReceiverServer mServer;
    private Tracker mTracker;
    private ArrayList<Fragment> mPages = new ArrayList<>();
    private long mRecordStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        mTracker = MultimicApplication.getDefaultTracker();

        ServerFragment serverFragment = new ServerFragment();
        final ClientListFragment clientListFragment = new ClientListFragment();

        mPages.add(serverFragment);
        mPages.add(clientListFragment);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setPageMargin(20);
        pager.setPageMarginDrawable(R.color.black);
        pager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        mTracker.setScreenName("Server Record");
                        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
                        break;

                    case 1:
                        mTracker.setScreenName("Client List");
                        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
                        break;
                }
            }
        });

        mServer = MediaReceiverServer.getInstance();
        mServer.setOnConnectedListener(new OnConnectedListener() {
            @Override
            public void onConnected(Socket socket) {
                clientListFragment.updateList();
                mTracker.send(
                        new HitBuilders.EventBuilder(
                                Analytics.CATEGORY_CONNECTION,
                                Analytics.ACTION_CONNECT)
                                .setLabel(Analytics.LABEL_TO_CLIENT)
                                .build());
            }
        });
        mServer.setOnDisconnectedListener(new OnDisconnectedListener() {
            @Override
            public void onDisconnected(Socket socket) {
                clientListFragment.updateList();
                mTracker.send(
                        new HitBuilders.EventBuilder(
                                Analytics.CATEGORY_CONNECTION,
                                Analytics.ACTION_DISCONNECT)
                                .setLabel(Analytics.LABEL_FROM_CLIENT)
                                .build());
            }
        });
        mServer.setRecordListener(new RecordListener() {
            @Override
            public void onRecordingStarted() {
                mRecordStartTime = System.currentTimeMillis();
                mTracker.send(
                        new HitBuilders.EventBuilder(
                                Analytics.CATEGORY_RECORDING,
                                Analytics.ACTION_RECORD_STARTED)
                                .build());
            }

            @Override
            public void onRecordingFinished() {
                long recordingLength = System.currentTimeMillis() - mRecordStartTime;
                Log.d(TAG, "Finished recording; time: " + recordingLength);
                mTracker.send(
                        new HitBuilders.EventBuilder(
                                Analytics.CATEGORY_RECORDING,
                                Analytics.ACTION_RECORD_FINISHED)
                                .setValue(recordingLength)
                                .build());
            }
        });
        mServer.start();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit?");
        builder.setMessage("Exit the server?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mServer.stop();
                finish();
                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mPages.get(position);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
