package org.spacebison.multimic.net;

import java.net.Socket;

/**
 * Created by cmb on 24.10.15.
 */
public interface OnConnectedListener {
    void onConnected(Socket socket);
}
