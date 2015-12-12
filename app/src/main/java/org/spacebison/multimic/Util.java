package org.spacebison.multimic;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cmb on 11.11.15.
 */
public class Util {
    public static ExecutorService  newMostCurrentTaskExecutor() {
        return new ThreadPoolExecutor(0, 1, 1, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());
    }

    public static ExecutorService  newMostCurrentTaskExecutor(ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(0, 1, 1, TimeUnit.MINUTES, new SynchronousQueue<Runnable>(), threadFactory);
    }

    public static <E> E getObjectAt(Iterable<E> collection, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index: " + index);
        }

        Iterator<E> iterator = collection.iterator();
        E element = null;
        for (int i = 0; i <= index; i++) {
            if (!iterator.hasNext()) {
                throw new IndexOutOfBoundsException("Index " + index + " of " + i);
            } else {
                element = iterator.next();
            }
        }

        return element;
    }

    public static int getThemeColor(Context context, int attrId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attrId, typedValue, true);
        return typedValue.data;
    }
}
