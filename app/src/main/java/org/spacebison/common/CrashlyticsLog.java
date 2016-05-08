package org.spacebison.common;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

/**
 * Created by cmb on 24.04.16.
 */
public class CrashlyticsLog {
    /**
     * Send a {@link Log#VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        Crashlytics.log(Log.VERBOSE, tag, msg);
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    public static boolean isLoggable(String s, int i) {
        return Log.isLoggable(s, i);
    }

    /**
     * Send a {@link Log#WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        Crashlytics.log(Log.WARN, tag, msg);
    }

    /**
     * Send a {@link Log#WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void w(String tag, String msg, Throwable tr) {
        Crashlytics.log(Log.WARN, tag, msg);
        Crashlytics.logException(tr);
    }

    /**
     * Send a {@link Log#ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        Crashlytics.log(Log.ERROR, tag, msg);
        Crashlytics.logException(tr);
    }

    public static void wtf(String tag, String msg) {
        Crashlytics.log(Log.ERROR, tag, msg);
    }

    /**
     * Send a {@link Log#DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
        Crashlytics.log(Log.DEBUG, tag, msg);
    }

    /**
     * Low-level logging call.
     * @param priority The priority/type of this log message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of bytes written.
     */
    public static void prvoidln(int priority, String tag, String msg) {
        Crashlytics.log(priority, tag, msg);
    }

    public static void w(String tag, Throwable tr) {
        Crashlytics.log(Log.WARN, tag, tr.toString());
        Crashlytics.logException(tr);
    }

    /**
     * Send a {@link Log#VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void v(String tag, String msg, Throwable tr) {
        Crashlytics.log(Log.VERBOSE, tag, msg);
        Crashlytics.logException(tr);
    }

    public static void wtf(String tag, Throwable tr) {
        Crashlytics.log(Log.ERROR, tag, tr.toString());
        Crashlytics.logException(tr);
    }

    /**
     * Send a {@link Log#DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void d(String tag, String msg, Throwable tr) {
        Crashlytics.log(Log.DEBUG, tag, msg);
        Crashlytics.logException(tr);
    }

    /**
     * Send an {@link Log#ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
        Crashlytics.log(Log.ERROR, tag, msg);
    }

    /**
     * Send an {@link Log#INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        Crashlytics.log(Log.INFO, tag, msg);
    }

    /**
     * Send a {@link Log#INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void i(String tag, String msg, Throwable tr) {
        Crashlytics.log(Log.INFO, tag, msg);
        Crashlytics.logException(tr);
    }

    public static void wtf(String tag, String msg, Throwable tr) {
        Crashlytics.log(Log.ERROR, tag, msg);
        Crashlytics.logException(tr);
    }
}
