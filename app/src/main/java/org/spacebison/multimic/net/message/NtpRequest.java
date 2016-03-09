package org.spacebison.multimic.net.message;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by cmb on 10.02.16.
 */
public class NtpRequest extends Message {
    @Expose
    @SerializedName("requestSendTime")
    public long requestSendTime;
    @Expose
    @SerializedName("requestReceiveTime")
    public long requestReceiveTime;

    public NtpRequest() {
    }

    public NtpRequest(long requestSendTime) {
        super(Type.NTP_REQUEST);
        this.requestSendTime = requestSendTime;
    }

    @Override
    public String toString() {
        return "NtpRequest{" +
                "requestSendTime=" + requestSendTime +
                '}';
    }
}
