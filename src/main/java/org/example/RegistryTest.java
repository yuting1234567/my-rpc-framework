package org.example;

import org.example.registry.Registry;
import org.example.registry.ZkRegistry;
import org.example.service.CalcService;

import java.net.InetSocketAddress;

public class RegistryTest {

    private static final String SERVICE = CalcService.class.getName();

    public static void main(String[] args) throws Exception {

        Registry registry = new ZkRegistry("localhost:2181");
//        registry.register(SERVICE, new InetSocketAddress("127.0.0.1", 8080));
//        System.out.println("查到的地址：" + registry.lookup(SERVICE));
//        System.out.println("按回车退出（退出后去 zkCli 看节点还在不在）");
//        System.in.read();
//        registry.close();

        while (true) {
            System.out.println("当前可用：" + registry.lookup(SERVICE));
            Thread.sleep(2000);
        }

    }
}
