package org.example.client;

import org.example.common.*;
import org.example.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    //重试机制
    private static int maxRetry = 3;

    private static Transport transport;
    private static Serializer serializer;

    public static void init(Transport t, SerializerType type){
        transport = t;
        serializer = type.getSerializer();
    }

    public static Serializer getSerializer(){
        return serializer;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> serviceClass){
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                (proxy, method, args) -> {
                    Request req = new Request(
                            serviceClass.getSimpleName(),
                            method.getName(),
                            method.getParameterTypes(),
                            args
                    );

                    long totalStart = System.currentTimeMillis();
                    Exception lastException = null;

                    for (int i = 1; i <= maxRetry;i++){
                        try {
                            logger.info("RPC 第{}次尝试: {}#{}",
                                        i,
                                        serviceClass.getSimpleName(),
                                        method.getName());

                            Response resp = transport.send(req);

                            if (resp.getError() != null){
                                throw new RuntimeException(resp.getError());
                            }

                            long totalCost = System.currentTimeMillis() - totalStart;

                            logger.info("RPC调用成功: {}#{}, 总耗时: {}ms",
                                        serviceClass.getSimpleName(),
                                        method.getName(),
                                        totalCost);

                            return resp.getData();
                        }catch (Exception e){
                            lastException = e;

                            logger.warn("RPC第{}次失败: {}#{}",
                                        i,
                                        serviceClass.getSimpleName(),
                                        method.getName(),
                                        e);

                            if(i == maxRetry) {
                                long totalCost = System.currentTimeMillis() - totalStart;

                                logger.error("RPC最终失败: {}#{}, 总耗时: {}ms",
                                             serviceClass.getSimpleName(),
                                             method.getName(),
                                             totalCost);

                                throw new RuntimeException("RPC 调用失败", e);
                            }
                        }
                    }
                    throw new RuntimeException(lastException);

                });
    }
}
