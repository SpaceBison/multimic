package org.spacebison.multimic.net.message;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by cmb on 10.02.16.
 */
public class StartRecord extends Message {
    @Expose
    @SerializedName("time")
    public long time;

    public StartRecord() {
    }

    public StartRecord(long time) {
        super(Type.START_RECORD);
        this.time = time;
    }

    @Override
    public String toString() {
        return "StartRecord{}";
    }
}
