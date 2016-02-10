package org.spacebison.multimic.net.message;

/**
 * Created by cmb on 10.02.16.
 */
public class NtpResponse implements Message {
    public long requestSendTime;
    public long requestReceiveTime;
    public long responseSendTime;

    public NtpResponse(long requestSendTime, long requestReceiveTime, long responseSendTime) {
        this.requestSendTime = requestSendTime;
        this.requestReceiveTime = requestReceiveTime;
        this.responseSendTime = responseSendTime;
    }

    @Override
    public Type getType() {
        return Type.NTP_RESPONSE;
    }
}
