package org.example.transport;

import org.example.common.Request;
import org.example.common.Response;

public interface Transport {
    Response send(Request request) throws Exception;

    void close();
}
