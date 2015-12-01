package org.spacebison.multimic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cmb on 11.11.15.
 */
public class Util {
    public static ExecutorService  newMostCurrentTaskExecutor() {
        return new ThreadPoolExecutor(0, 1, 1, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());
    }
}
