package org.spacebison.multimic.net;

/**
 * Created by cmb on 28.10.15.
 */
public class Protocol {
    public static final byte START_RECORD = 1;
    public static final byte STOP_RECORD = 2;
    public static final byte NTP_REQUEST = 3;
    public static final String DISCOVERY_MULTICAST_GROUP = "239.6.6.6";
    public static final int DISCOVERY_MULTICAST_PORT = 23966;
    public static final int SERVER_PORT = 23969;
}
