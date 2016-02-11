package org.spacebison.multimic.net.message;

/**
 * Created by cmb on 10.02.16.
 */
public class NtpResponse implements Message {
    public long requestSendTime;
    public long requestReceiveTime;
    public long responseSendTime;
    public long responseReceiveTime;

    public NtpResponse(long requestSendTime, long requestReceiveTime, long responseSendTime) {
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
    public Type getType() {
        return Type.NTP_RESPONSE;
    }
}
