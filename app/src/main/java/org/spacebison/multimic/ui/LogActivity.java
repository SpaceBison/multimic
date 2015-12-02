package org.spacebison.multimic.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import org.spacebison.multimic.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogActivity extends Activity {
    TextView mLogText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        mLogText = (TextView) findViewById(R.id.logTextView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new AsyncTask<Void, CharSequence, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Process process = Runtime.getRuntime().exec("logcat -d");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        publishProgress(getColoredLine(line));
                    }
                } catch (IOException e) {
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(CharSequence... values) {
                mLogText.append(values[0]);
                mLogText.append("\n");
            }
        }.execute();
    }

    private static CharSequence getColoredLine(String line) {
        int color;
        switch (line.charAt(0)) {
            case 'E':
                color = Color.RED;
                break;

            case 'W':
                color = Color.BLUE;
                break;

            case 'D':
                color = Color.BLACK;
                break;

            default:
                return line;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(line);
        ssb.setSpan(new ForegroundColorSpan(color), 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }
}
