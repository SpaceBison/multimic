package org.spacebison.multimic.io;

import org.spacebison.common.CrashlyticsLog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cmb on 18.11.15.
 */
public class JsonOutputStream extends FilterOutputStream {
    private static final String CRLF = "\r\n";
    private static final String TAG = "JsonOutputStream";
    private Gson mGson = new GsonBuilder().disableHtmlEscaping().create();

    public JsonOutputStream(OutputStream outputStream) {
        super(outputStream);
    }

    public void write(Object object) throws IOException {
        String json = mGson.toJson(object);
        CrashlyticsLog.v(TAG, "Write json: " + json);
        out.write(json.getBytes("UTF-8"));
        out.write(CRLF.getBytes("UTF-8"));
    }
}
