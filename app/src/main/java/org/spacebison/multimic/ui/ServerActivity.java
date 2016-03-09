package org.spacebison.multimic.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by cmb on 08.03.16.
 */
public class ServerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new ServerControlFragment())
                .commit();
    }
}
