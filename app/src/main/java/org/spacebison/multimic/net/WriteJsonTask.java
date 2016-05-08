package org.spacebison.multimic.net;

import org.spacebison.common.CrashlyticsLog;

import org.spacebison.multimic.io.JsonOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cmb on 19.11.15.
 */
public class WriteJsonTask implements Runnable {
    private static final String TAG = "WriteJsonTask";
    private final OutputStream mOutputStream;
    private final Object mObject;

    public WriteJsonTask(OutputStream outputStream, Object object) {
        mOutputStream = outputStream;
        mObject = object;
    }

    @Override
    public void run() {
        synchronized (mOutputStream) {
            try {
                CrashlyticsLog.v(TAG, "Writing " + mObject);
                JsonOutputStream output = new JsonOutputStream(mOutputStream);
                output.write(mObject);
                output.flush();
            } catch (IOException e) {
            }
        }
    }
}
