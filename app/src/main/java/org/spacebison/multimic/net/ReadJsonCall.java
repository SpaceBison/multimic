package org.spacebison.multimic.net;

import org.spacebison.common.CrashlyticsLog;

import org.spacebison.multimic.gson.GsonHelper;
import org.spacebison.multimic.io.JsonInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * Created by cmb on 18.11.15.
 */
public class ReadJsonCall<T> implements Callable<T> {
    private static final String TAG = "ReadJsonCall";
    private final InputStream mInputStream;
    private final Class<T> mObjectClass;

    public ReadJsonCall(InputStream inputStream, Class<T> objectClass) {
        mInputStream = inputStream;
        mObjectClass = objectClass;
    }

    @Override
    public T call() throws IOException {
        synchronized (mInputStream) {
            CrashlyticsLog.v(TAG, "Reading json");
            JsonInputStream jsonInputStream = new JsonInputStream(mInputStream, GsonHelper.getGson());
            return jsonInputStream.readJsonObject(mObjectClass);
        }
    }
}
