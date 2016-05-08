package org.spacebison.multimic.net.discovery.message;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by cmb on 09.02.16.
 */
public class DiscoveryRequest implements Serializable, Parcelable {
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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.version);
    }

    protected DiscoveryRequest(Parcel in) {
        this.version = in.readInt();
    }

    public static final Parcelable.Creator<DiscoveryRequest> CREATOR = new Parcelable.Creator<DiscoveryRequest>() {
        @Override
        public DiscoveryRequest createFromParcel(Parcel source) {
            return new DiscoveryRequest(source);
        }

        @Override
        public DiscoveryRequest[] newArray(int size) {
            return new DiscoveryRequest[size];
        }
    };
}
