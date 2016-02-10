package org.spacebison.multimic.net.discovery.message;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by cmb on 24.10.15.
 */
public class ResolvedService implements Serializable {
    public String name;
    public int version;
    public InetAddress address;
    public int port;

    public ResolvedService(String name, int version, InetAddress address, int port) {
        this.name = name;
        this.version = version;
        this.address = address;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResolvedService that = (ResolvedService) o;

        if (version != that.version) return false;
        if (port != that.port) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return !(address != null ? !address.equals(that.address) : that.address != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + version;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "ResolvedService{" + name + "; ver. " + version + "; " + address + ':' + port + '}';
    }
}
