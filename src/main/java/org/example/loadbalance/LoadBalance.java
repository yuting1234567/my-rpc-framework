package org.example.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalance {
    InetSocketAddress select(List<InetSocketAddress> addresses);
}
