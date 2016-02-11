package org.spacebison.multimic.net;

import org.spacebison.multimic.net.message.NtpResponse;

import java.net.Socket;

/**
 * Created by cmb on 11.02.16.
 */
public class ReadNtpResponseCall extends ReadMessageCall<NtpResponse> {
    private static final String TAG = "ReadNtpResponseCall";

    public ReadNtpResponseCall(Socket socket) {
        super(socket);
    }

    @Override
    public NtpResponse call() throws Exception {
        NtpResponse ntpResponse = super.call();
        ntpResponse.responseReceiveTime = System.currentTimeMillis();
        return ntpResponse;
    }
}
