package org.spacebison.multimic.model;

import org.spacebison.multimic.net.discovery.MulticastServiceResolver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cmb on 11.02.16.
 */
public class MultimicClient {
    private static final String TAG = "MultimicClient";
    private static final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private Server mServer;

    public static void resolveServers(int timeout, MulticastServiceResolver.Listener listener) {
        new MulticastServiceResolver(Config.DISCOVERY_MULTICAST_GROUP, Config.DISCOVERY_MULTICAST_PORT, listener)
                .resolve(timeout);
    }

    public void connect(Server server) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public interface Listener {
        void onConnected(Server server);
    }
}
