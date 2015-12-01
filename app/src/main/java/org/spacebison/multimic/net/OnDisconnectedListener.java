package org.spacebison.multimic.net;

import java.net.Socket;

/**
 * Created by cmb on 2015-11-11.
 */
public interface OnDisconnectedListener {
    void onDisconnected(Socket socket);
}
