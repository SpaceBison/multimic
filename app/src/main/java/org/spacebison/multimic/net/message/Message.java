package org.spacebison.multimic.net.message;

import java.io.Serializable;

/**
 * Created by cmb on 10.02.16.
 */
public interface Message extends Serializable {
    public enum Type {
        START_RECORD,
        STOP_RECORD,
        NTP_REQUEST,
        NTP_RESPONSE,
        HELLO
    }

    Type getType();
}
