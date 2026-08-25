package org.example.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZkRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(ZkRegistry.class);

    private static final String ROOT = "/my-rpc";

    private final CuratorFramework client;

    private final Map<String, List<InetSocketAddress>> cache = new ConcurrentHashMap<>();
    private final Map<String, CuratorCache> watchers = new ConcurrentHashMap<>();

    public ZkRegistry(String zkAddress) {
        this.client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(30000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        this.client.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在注销服务……");
            close();
        }));
    }

    @Override
    public void register(String serviceName, InetSocketAddress address) {
        String path = ROOT + "/" + serviceName + "/" + address.getHostString() + ":" + address.getPort();
        try {
            client.create()
                    .creatingParentsIfNeeded()  //父路径不存在就自动建
                    .withMode(CreateMode.EPHEMERAL)  //临时节点
                    .forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            try{
                client.delete().forPath(path);
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path);
            }catch (Exception ex){
                throw new RuntimeException("注册失败：" + path, ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("注册失败：" + path, e);
        }
        logger.info("已注册： {}", path);
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        //还没订阅过 -> 订阅一次（内部会拉全量 + 挂监听）
        watchers.computeIfAbsent(serviceName, this::subscribe);
        return cache.getOrDefault(serviceName, Collections.emptyList());
    }

    private CuratorCache subscribe(String serviceName) {
        String path = ROOT + "/" +serviceName;
        CuratorCache curatorCache = CuratorCache.build(client, path);

        CuratorCacheListener listener = CuratorCacheListener.builder()
                                                            .afterInitialized()   //初始同步阶段的事件全部忽略
                                                            .forCreates(node -> refresh(serviceName))   //只关心新增
                                                            .forDeletes(node -> refresh(serviceName))   //和删除
                                                            .build();

        curatorCache.listenable().addListener(listener);
        curatorCache.start();

        refresh(serviceName);   //初始数据由自己拉一次，精确可控
        return curatorCache;
    }

    private void refresh(String serviceName) {
        String path = ROOT + "/" + serviceName;
        try {
            List<String> children = client.getChildren().forPath(path);
            List<InetSocketAddress> list = new ArrayList<>();
            for (String child : children) {
                String[] parts = child.split(":");
                list.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
            }
            list.sort(Comparator.comparing(InetSocketAddress::toString));
            cache.put(serviceName, list);
            logger.info("注册中心地址更新：{} -> {}",serviceName,list);
        }catch (Exception e){
            logger.info("刷新地址列表失败：{}", path, e);
            //注意：这里不清空缓存，保留旧数据继续用
        }
    }

    @Override
    public void close() {
        watchers.values().forEach(CuratorCache::close);
        if (client != null && client.getState() == CuratorFrameworkState.STARTED) {
            client.close();
        }
    }
}
