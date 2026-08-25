package org.example.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalance implements LoadBalance {
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses) {
        int index = ThreadLocalRandom.current().nextInt(addresses.size());
        return addresses.get(index);
    }
}
