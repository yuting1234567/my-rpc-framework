package org.example;

import org.example.loadbalance.LoadBalance;
import org.example.loadbalance.RandomLoadBalance;
import org.example.loadbalance.RoundRobinLoadBalance;

import java.net.InetSocketAddress;
import java.util.List;

public class LoadBalanceTest {
    public static void main(String[] args) {
        List<InetSocketAddress> addrs = List.of(
                new InetSocketAddress("127.0.0.1", 8080),
                new InetSocketAddress("127.0.0.1", 8081));

        LoadBalance lb = new RoundRobinLoadBalance();
        for (int i = 0; i < 10; i++) {
            System.out.print(lb.select(addrs).getPort() + " ");
        }
    }
}
