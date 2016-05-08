package org.spacebison.multimic.model;

import java.io.Serializable;
import java.net.Socket;

/**
 * Created by cmb on 11.02.16.
 */
public class Client implements Serializable {
    public Socket socket;
    public String name;
    public long timeOffset;
    public long delay;

    public Client(Socket socket, String name, long timeOffset, long delay) {
        this.socket = socket;
        this.name = name;
        this.timeOffset = timeOffset;
        this.delay = delay;
    }

    @Override
    public String toString() {
        return "Client{" +
                "name='" + name + '\'' +
                ", socket=" + socket +
                '}';
    }
}
