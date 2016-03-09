package org.spacebison.multimic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Created by cmb on 13.02.16.
 */
public class Prefs {
    private static final String PREFS_NAME = "MultiMicPrefs";
    private static final String KEY_NAME = "name";

    private static Prefs sInstance;

    private final SharedPreferences mPreferences = MultimicApplication.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

    private Prefs() {}

    public static Prefs getInstance() {
        if (sInstance == null) {
            synchronized (Prefs.class) {
                sInstance = new Prefs();
            }
        }
        return sInstance;
    }

    public void setName(String name) {
        mPreferences.edit().putString(KEY_NAME, name).apply();
    }

    public String getName() {
        return mPreferences.getString(KEY_NAME, Build.MODEL);
    }
}
