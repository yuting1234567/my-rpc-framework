package org.example.client;

import org.example.common.SerializerType;
import org.example.loadbalance.LoadBalance;
import org.example.loadbalance.RoundRobinLoadBalance;
import org.example.registry.Registry;
import org.example.registry.ZkRegistry;
import org.example.service.CalcService;
import org.example.transport.NettyClientTransport;
import org.example.transport.Transport;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientApp {
    public static void main(String[] args) throws Exception {

        Registry registry = new ZkRegistry("localhost:2181");
        LoadBalance loadBalance = new RoundRobinLoadBalance();

        Transport transport = new NettyClientTransport(registry, loadBalance, SerializerType.JSON);
        RpcClient.init(transport);

        //同步
        CalcService sync = RpcClient.getProxy(CalcService.class);
        System.out.println("同步：1 + 1 = " + sync.add(1, 1));

        //异步：两个请求并行
        CalcService async = RpcClient.getAsyncProxy(CalcService.class);

        async.add(1,2);
        CompletableFuture<Integer> f1 = RpcContext.getFuture();

        async.add(3,4);
        CompletableFuture<Integer> f2 = RpcContext.getFuture();

        System.out.println("两个请求都发出去了，还没等任何东西");

        System.out.println("异步结果：" + f1.get() + ", " + f2.get());

        //观察回调跑在哪个线程
        async.add(10,20);
        RpcContext.<Integer>getFuture().thenAccept(r -> System.out.println("[thenAccept] 结果=" + r + ",线程=" + Thread.currentThread().getName()));

        ExecutorService bizPool  = Executors.newFixedThreadPool(4);

        async.add(30,40);
        RpcContext.<Integer>getFuture().thenAcceptAsync(r -> System.out.println("[thenAcceptAsync] 结果=" + r + ",线程=" + Thread.currentThread().getName()), bizPool);

        Thread.sleep(1000);   //等异步回调招待完
        bizPool.shutdown();
        transport.close();
    }
}
