package org.spacebison.multimic.net.discovery;

import java.net.InetAddress;

/**
 * Created by cmb on 24.10.15.
 */
public interface OnServiceResolvedListener {
    void onServiceResolved(InetAddress address, int port);
    void onResolveEnded();
}
