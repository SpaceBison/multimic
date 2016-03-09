package org.spacebison.multimic.net.message;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by cmb on 10.02.16.
 */
public class NtpResponse extends Message {
    @Expose
    @SerializedName("requestSendTime")
    public long requestSendTime;
    @Expose
    @SerializedName("requestReceiveTime")
    public long requestReceiveTime;
    @Expose
    @SerializedName("responseSendTime")
    public long responseSendTime;
    @Expose
    @SerializedName("responseReceiveTime")
    public long responseReceiveTime;

    public NtpResponse() {
    }

    public NtpResponse(NtpRequest request, long responseSendTime) {
        this(request.requestSendTime, request.requestReceiveTime, responseSendTime);
    }

    public NtpResponse(long requestSendTime, long requestReceiveTime, long responseSendTime) {
        super(Type.NTP_RESPONSE);
        this.requestSendTime = requestSendTime;
        this.requestReceiveTime = requestReceiveTime;
        this.responseSendTime = responseSendTime;
    }

    public long getOffset() {
        return (requestReceiveTime - requestSendTime) + (responseSendTime - requestReceiveTime);
    }

    public long getDelay() {
        return (responseReceiveTime - requestSendTime) - (responseSendTime - requestReceiveTime);
    }

    @Override
    public String toString() {
        return "NtpResponse{" +
                "requestSendTime=" + requestSendTime +
                ", requestReceiveTime=" + requestReceiveTime +
                ", responseSendTime=" + responseSendTime +
                ", responseReceiveTime=" + responseReceiveTime +
                '}';
    }
}
