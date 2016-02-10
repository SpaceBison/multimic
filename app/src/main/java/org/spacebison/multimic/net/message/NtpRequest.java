package org.spacebison.multimic.net.message;

/**
 * Created by cmb on 10.02.16.
 */
public class NtpRequest implements Message {
    public long requestSendTime;

    public NtpRequest(long requestSendTime) {
        this.requestSendTime = requestSendTime;
    }

    @Override
    public Type getType() {
        return Type.NTP_REQUEST;
    }
}
