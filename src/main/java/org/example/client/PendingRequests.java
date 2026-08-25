package org.example.client;

import org.example.common.RpcMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PendingRequests {

    //所有"已发出、还没收到响应"的请求都在这
    private static final Map<Long, CompletableFuture<RpcMessage>> MAP = new ConcurrentHashMap<>();

    private static final AtomicLong ID_GEN = new AtomicLong(0);

    //取一个新的请求编号
    public static long nextId() {
        return ID_GEN.incrementAndGet();
    }

    //登记：我发了 id 号请求，给我一个盒子
    public static CompletableFuture<RpcMessage> register(long requestId) {
        CompletableFuture<RpcMessage> future = new CompletableFuture<>();
        MAP.put(requestId, future);
        return future;
    }

    //响应回来了：找到对应的盒子，把结果放进去
    public static void complete(RpcMessage response){
        CompletableFuture<RpcMessage> future = MAP.remove(response.getRequestId());
        if(future != null){
            future.complete(response);
        }
        //找不到 = 这个请求已经超时被清理了，响应来晚了，直接丢弃
    }

    //超时了，把盒子撤掉，别占内存
    public static void remove(long requestId){
        MAP.remove(requestId);
    }
}
