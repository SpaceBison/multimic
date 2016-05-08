package org.spacebison.multimic.net.discovery.message;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by cmb on 24.10.15.
 */
public class ResolvedService implements Serializable, Parcelable {
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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.version);
        dest.writeSerializable(this.address);
        dest.writeInt(this.port);
    }

    protected ResolvedService(Parcel in) {
        this.name = in.readString();
        this.version = in.readInt();
        this.address = (InetAddress) in.readSerializable();
        this.port = in.readInt();
    }

    public static final Parcelable.Creator<ResolvedService> CREATOR = new Parcelable.Creator<ResolvedService>() {
        @Override
        public ResolvedService createFromParcel(Parcel source) {
            return new ResolvedService(source);
        }

        @Override
        public ResolvedService[] newArray(int size) {
            return new ResolvedService[size];
        }
    };
}
