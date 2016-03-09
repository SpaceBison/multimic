package org.spacebison.common;

import android.app.Service;
import android.os.Binder;

/**
 * Created by cmb on 05.02.16.
 */
public class ServiceBinder<T extends Service> extends Binder {
    private T mService;

    public ServiceBinder(T service) {
        mService = service;
    }

    public T getService() {
        return mService;
    }
}
