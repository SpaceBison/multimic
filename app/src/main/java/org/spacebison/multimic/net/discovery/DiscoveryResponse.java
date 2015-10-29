package org.spacebison.multimic.net.discovery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by cmb on 24.10.15.
 */
public class DiscoveryResponse {
    private static final int PORT_BYTES = 4;
    private static final int ADDRESS_BYTES = 16;
    private InetAddress mAddress;
    private int mPort;

    public DiscoveryResponse(InetAddress address, int port) {
        mAddress = address;
        mPort = port;
    }

    public byte[] getBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(ByteBuffer.allocate(PORT_BYTES).putInt(mPort).array());
            baos.write(mAddress.getAddress());
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public InetAddress getAddress() {
        return mAddress;
    }

    public int getPort() {
        return mPort;
    }

    public static int maxByteCount() {
        return PORT_BYTES + ADDRESS_BYTES;
    }

    public static DiscoveryResponse fromBytes(byte[] bytes) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            byte[] addressBytes = new byte[ADDRESS_BYTES];
            byte[] portBytes = new byte[PORT_BYTES];
            bais.read(portBytes);
            bais.read(addressBytes);

            return new DiscoveryResponse(InetAddress.getByAddress(addressBytes), ByteBuffer.allocate(PORT_BYTES).getInt());
    }
}
