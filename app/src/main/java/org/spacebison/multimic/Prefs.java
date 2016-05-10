package org.spacebison.multimic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * Created by cmb on 13.02.16.
 */
public class Prefs {
    private static final String PREFS_NAME = "MultiMicPrefs";
    private static final String KEY_NAME = "name";

    private static Prefs sInstance;

    private final SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(MultimicApplication.getContext());
    private final Context mContext = MultimicApplication.getContext();

    private Prefs() {
        setDefaults();
    }



    private void setDefaults() {
        if (!mPreferences.contains(mContext.getString(R.string.prefs_name_key))) {
            mPreferences.edit().putString(mContext.getString(R.string.prefs_name_key), Build.MODEL).apply();
        }
    }

    public static Prefs getInstance() {
        if (sInstance == null) {
            synchronized (Prefs.class) {
                sInstance = new Prefs();
            }
        }
        return sInstance;
    }

    public void setName(String name) {
        mPreferences.edit().putString(mContext.getString(R.string.prefs_name_key), name).apply();
    }

    public String getName() {
        return mPreferences.getString(mContext.getString(R.string.prefs_name_key), Build.MODEL);
    }

    public SharedPreferences getPreferences() {
        return mPreferences;
    }
}
