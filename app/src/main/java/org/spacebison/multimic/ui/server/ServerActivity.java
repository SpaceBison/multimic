package org.spacebison.multimic.ui.server;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import org.spacebison.multimic.MediaReceiverServer;
import org.spacebison.multimic.R;

/**
 * Created by cmb on 25.10.15.
 */
public class ServerActivity extends AppCompatActivity {
    private static final String TAG = "cmb.ServerActivity";
    private ViewPager mPager;
    private MediaReceiverServer mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

        mServer = MediaReceiverServer.getInstance();
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
            switch(position) {
                case 0:
                    return new ServerFragment();

                case 1:
                    return new ClientListFragment();

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
