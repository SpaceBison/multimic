package org.spacebison.multimic.net.message;

/**
 * Created by cmb on 11.02.16.
 */
public class Hello implements Message {
    public String name;

    public Hello(String name) {
        this.name = name;
    }

    @Override
    public Type getType() {
        return Type.HELLO;
    }
}
