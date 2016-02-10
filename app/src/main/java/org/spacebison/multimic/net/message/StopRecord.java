package org.spacebison.multimic.net.message;

/**
 * Created by cmb on 10.02.16.
 */
public class StopRecord implements Message {
    @Override
    public Type getType() {
        return Type.STOP_RECORD;
    }
}
