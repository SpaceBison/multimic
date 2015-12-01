package org.spacebison.multimic.net;

import java.net.Socket;

/**
 * Created by cmb on 11.11.15.
 */
public interface OnSocketBytesTransferredListener {
    void onSocketBytesTransferred(Socket socket, int bytes);
}
