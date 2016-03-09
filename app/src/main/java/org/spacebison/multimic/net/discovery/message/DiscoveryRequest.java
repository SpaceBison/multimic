package org.spacebison.multimic.net.discovery.message;

import java.io.Serializable;

/**
 * Created by cmb on 09.02.16.
 */
public class DiscoveryRequest implements Serializable {
    public int version = 1;

    public DiscoveryRequest(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DiscoveryRequest{" +
                "version=" + version +
                '}';
    }
}
