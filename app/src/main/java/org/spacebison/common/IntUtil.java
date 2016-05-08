package org.spacebison.common;

import android.os.Message;

/**
 * Created by cmb on 03.05.16.
 */
public class IntUtil {
    public static long getLong(int hi, int lo) {
        return ((long) hi << 32) | ((long) lo);
    }

    public static int getHighBytes(long val) {
        return (int) (val >>> 32);
    }

    public static int getLowBytes(long val) {
        return (int) (val & 0xFFFFFFF);
    }

    public static long getLongArg(Message message) {
        return getLong(message.arg1, message.arg2);
    }

    public static Message setLongArg(Message message, long arg) {
        message.arg1 = getHighBytes(arg);
        message.arg2 = getLowBytes(arg);
        return message;
    }
}
