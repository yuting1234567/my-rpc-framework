package org.example.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalance implements LoadBalance {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses) {
        int i = Math.abs(index.getAndIncrement() % addresses.size());
        return addresses.get(i);
    }
}
