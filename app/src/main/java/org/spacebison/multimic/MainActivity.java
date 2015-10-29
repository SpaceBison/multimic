package org.spacebison.multimic;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.spacebison.multimic.net.Client;
import org.spacebison.multimic.net.Server;
import org.spacebison.multimic.net.discovery.MulticastServiceProvider;

import java.io.IOException;
import java.net.InetAddress;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ViewGroup root = (ViewGroup) findViewById(R.id.root);

        final Button serverButton = new Button(this);
        final Button clientButton = new Button(this);

        serverButton.setText("server");
        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ServerActivity.class));
            }
        });
        root.addView(serverButton);

        clientButton.setText("client");
        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //c.start();
                startActivity(new Intent(MainActivity.this, ServerSearchActivity.class));
            }
        });
        root.addView(clientButton);
    }
}
