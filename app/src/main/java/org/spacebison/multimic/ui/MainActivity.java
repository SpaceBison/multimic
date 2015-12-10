package org.spacebison.multimic.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.ui.player.PlayerListActivity;
import org.spacebison.multimic.ui.server.ServerActivity;

public class MainActivity extends AppCompatActivity {
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTracker = MultimicApplication.getDefaultTracker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Main");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void clickClient(View view) {
        startActivity(new Intent(MainActivity.this, ServerSearchActivity.class));
    }

    public void clickServer(View view) {
        startActivity(new Intent(MainActivity.this, ServerActivity.class));
    }

    public void clickPlayer(View view) {
        startActivity(new Intent(MainActivity.this, PlayerListActivity.class));
    }
}
