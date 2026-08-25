package org.example.registry;

import java.net.InetSocketAddress;
import java.util.List;

public interface Registry {

    //Provider 启动时调用：我提供serviceName，我在 address
    void register(String serviceName, InetSocketAddress address);

    //Consumer 调用：serviceName 现在有哪些地址？
    List<InetSocketAddress> lookup(String serviceName);

    void close();
}
