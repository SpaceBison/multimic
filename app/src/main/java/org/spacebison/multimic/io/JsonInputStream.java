package org.spacebison.multimic.io;

import org.spacebison.common.CrashlyticsLog;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

/**
 * Created by cmb on 18.11.15.
 */
public class JsonInputStream extends FilterInputStream {
    private static final Pattern LINE_END_PATTERN = Pattern.compile("\r|\n|\r\n");
    private static final String TAG = "JsonInputStream";
    private final InputStreamReader mReader;
    private final Gson mGson;

    public JsonInputStream(InputStream inputStream) {
        this(inputStream, new Gson());
    }

    public JsonInputStream(InputStream inputStream, Gson gson) {
        super(inputStream);
        mGson = gson;
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(inputStream, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            CrashlyticsLog.w(TAG, e.toString());
            reader = new InputStreamReader(inputStream);
        } finally {
            mReader = reader;
        }
    }

    public <T> T readJsonObject(Class<T> classOfT) throws IOException {
        StringBuilder builder = new StringBuilder();
        T object = null;

        do {
            appendLine(builder);
            try {
                object = mGson.fromJson(builder.toString(), classOfT);
                CrashlyticsLog.v(TAG, "Read json: " + builder);
            } catch (JsonSyntaxException e) {
            }
        } while (object == null);
        return object;
    }

    private void appendLine(StringBuilder builder) throws IOException {
        int c = 0;
        do {
            c = mReader.read();
        } while (c == '\n' || c == '\r');

        builder.append((char)c);
        c = mReader.read();

        while (c != '\n' && c != '\r') {
            if (c == -1) {
                throw new IOException("EOT");
            }

            builder.append((char)c);
            c = mReader.read();
        }
    }

    @Override
    public void close() throws IOException {
        mReader.close();
    }
}
