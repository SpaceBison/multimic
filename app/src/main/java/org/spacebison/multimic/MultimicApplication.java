package org.spacebison.multimic;

import android.app.Application;
import android.content.Context;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.common.CrashlyticsLog;

import java.util.concurrent.ThreadFactory;

import io.fabric.sdk.android.Fabric;

/**
 * Created by cmb on 04.12.15.
 */
public class MultimicApplication extends Application {
    private static final String TAG = "cmb.MultimicApplication";
    private static final Object LOCK = new Object();
    private static Context sApplicationContext;
    private static Tracker sTracker;
    private static ThreadFactory sThreadFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        sApplicationContext = this;

        try {
            LOCK.notifyAll();
        } catch (IllegalMonitorStateException e) {
            CrashlyticsLog.w(TAG, e.toString());
        }

        Thread.UncaughtExceptionHandler handler = new ExceptionReporter(
                getDefaultTracker(),                                        // Currently used Tracker.
                Thread.getDefaultUncaughtExceptionHandler(),      // Current default uncaught exception handler.
                this);                                         // Context of the application.

        Thread.setDefaultUncaughtExceptionHandler(handler);
        new ToastServiceBroadcastReceiver().register(this);
    }

    

    synchronized public static Context getContext() {
        if (sApplicationContext == null) {
            try {
                LOCK.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return sApplicationContext;
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public static Tracker getDefaultTracker() {
        if (sTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(getContext());
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            sTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return sTracker;
    }

    synchronized public static ThreadFactory getAnalyticsThreadFactory() {
        if (sThreadFactory == null) {
            sThreadFactory = new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setUncaughtExceptionHandler(
                            new ExceptionReporter(
                                    sTracker,
                                    thread.getUncaughtExceptionHandler(),
                                    getContext()));
                    return thread;
                }
            };
        }
        return sThreadFactory;
    }
}
