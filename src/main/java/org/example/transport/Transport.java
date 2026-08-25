package org.example.transport;

import org.example.common.Request;
import org.example.common.Response;

import java.util.concurrent.CompletableFuture;

public interface Transport {
    Response send(Request request) throws Exception;
    CompletableFuture<Response> sendAsync(Request request) throws Exception;
    void close();
}
