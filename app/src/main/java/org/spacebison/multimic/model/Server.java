package org.spacebison.multimic.model;

import java.net.Socket;

/**
 * Created by cmb on 11.02.16.
 */
public class Server {
    public Socket socket;
    public String name;

    public Server(Socket socket, String name) {
        this.socket = socket;
        this.name = name;
    }
}
