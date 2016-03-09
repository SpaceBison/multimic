package org.spacebison.multimic.net.message;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by cmb on 11.02.16.
 */
public class Hello extends Message {
    @Expose
    @SerializedName("name")
    public String name;

    public Hello(String name) {
        super(Type.HELLO);
        this.name = name;
    }

    @Override
    public String toString() {
        return "Hello{" +
                "name='" + name + '\'' +
                '}';
    }
}
