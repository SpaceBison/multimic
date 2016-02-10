package org.spacebison.multimic.net.message;

/**
 * Created by cmb on 10.02.16.
 */
public class StartRecord implements Message {
    @Override
    public Type getType() {
        return Type.START_RECORD;
    }
}
