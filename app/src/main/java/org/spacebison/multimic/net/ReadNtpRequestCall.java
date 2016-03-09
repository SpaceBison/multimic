package org.spacebison.multimic.net;

import org.spacebison.multimic.net.message.NtpRequest;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by cmb on 11.02.16.
 */
public class ReadNtpRequestCall extends ReadJsonCall<NtpRequest> {

    public ReadNtpRequestCall(InputStream inputStream) throws IOException {
        super(inputStream, NtpRequest.class);
    }

    @Override
    public NtpRequest call() throws IOException {
        NtpRequest ntpRequest = super.call();
        ntpRequest.requestReceiveTime = System.currentTimeMillis();
        return ntpRequest;
    }
}
