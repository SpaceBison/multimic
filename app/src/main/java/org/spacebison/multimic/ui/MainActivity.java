package org.spacebison.multimic.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.spacebison.multimic.R;
import org.spacebison.multimic.ui.server.ServerActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void clickClient(View view) {
        startActivity(new Intent(MainActivity.this, ServerSearchActivity.class));
    }

    public void clickServer(View view) {
        startActivity(new Intent(MainActivity.this, ServerActivity.class));
    }
}