package org.spacebison.multimic.net.message;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by cmb on 10.02.16.
 */
public class Message implements Serializable {
    @Expose
    @SerializedName("type")
    private Type mType = Type.UNKNOWN;

    public Message() {}

    public Message(Type type) {
        mType = type;
    }

    public Type getType() {
        return mType;
    }

    public enum Type {
        START_RECORD,
        STOP_RECORD,
        NTP_REQUEST,
        NTP_RESPONSE,
        HELLO,
        UNKNOWN
    }
}
