package org.spacebison.multimic.net.discovery;

import java.net.DatagramPacket;

/**
 * Created by cmb on 29.10.15.
 */
public interface OnRequestReceivedListener {
    void onRequestReceived(DatagramPacket packer);
}
