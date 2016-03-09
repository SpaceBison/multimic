package org.spacebison.multimic.net;

import org.spacebison.multimic.net.message.NtpResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by cmb on 11.02.16.
 */
public class ReadNtpResponseCall extends ReadJsonCall<NtpResponse> {

    public ReadNtpResponseCall(InputStream inputStream) throws IOException {
        super(inputStream, NtpResponse.class);
    }

    @Override
    public NtpResponse call() throws IOException {
        NtpResponse ntpResponse = super.call();
        ntpResponse.responseReceiveTime = System.currentTimeMillis();
        return ntpResponse;
    }
}
