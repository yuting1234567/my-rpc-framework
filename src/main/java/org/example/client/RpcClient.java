package org.example.client;

import org.example.common.*;
import org.example.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    //重试机制
    private static int maxRetry = 3;

    private static Transport transport;

    public static void init(Transport t){
        transport = t;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> serviceClass){
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                (proxy, method, args) -> {
                    Request req = new Request(
                            serviceClass.getName(),
                            method.getName(),
                            method.getParameterTypes(),
                            args
                    );

                    long totalStart = System.currentTimeMillis();
                    Exception lastException = null;

                    for (int i = 1; i <= maxRetry;i++){

                        Response resp;

                        try {
                            logger.info("RPC 第{}次尝试: {}#{}",
                                        i,
                                        serviceClass.getSimpleName(),
                                        method.getName());

                            resp = transport.send(req);


                        }catch (Exception e){
                            lastException = e;

                            logger.warn("RPC第{}次失败: {}#{}",
                                        i,
                                        serviceClass.getSimpleName(),
                                        method.getName(),
                                        e);

                            continue;
                        }

                        //走到这，说明网络没问题

                        if (resp.getError() != null){
                            throw new RpcBizException(resp.getError());
                        }
                        long totalCost = System.currentTimeMillis() - totalStart;

                        logger.info("RPC调用成功: {}#{}, 总耗时: {}ms",
                                    serviceClass.getSimpleName(),
                                    method.getName(),
                                    totalCost);

                        return resp.getData();
                    }
                    throw new RuntimeException("RPC 调用失败，重试 " + maxRetry +" 次", lastException);

                });
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAsyncProxy(Class<T> serviceClass){
        return (T)Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                (proxy, method, args) -> {
                    Request req = new Request(
                            serviceClass.getName(),
                            method.getName(),
                            method.getParameterTypes(),
                            args);

                    CompletableFuture<Object> future = transport.sendAsync(req)
                                                                .thenApply(resp -> {
                                if (!resp.isSuccess()){
                                    throw new RpcBizException(resp.getError());
                                }
                                return resp.getData();
                            });
                    RpcContext.setFuture(future);
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0d;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        if (type == boolean.class) return false;
        return null;
    }
}
