package org.example.client;

import java.util.concurrent.CompletableFuture;

public class RpcContext {

    private static final ThreadLocal<CompletableFuture<?>> FUTURE = new ThreadLocal<>();
    static void setFuture(CompletableFuture<?> future){
        FUTURE.set(future);
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> getFuture(){
        CompletableFuture<T> future = (CompletableFuture<T>) FUTURE.get();
        FUTURE.remove();   //取走就清掉
        return future;
    }

}
