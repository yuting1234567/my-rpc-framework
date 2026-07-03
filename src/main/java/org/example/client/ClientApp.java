package org.example.client;

import org.example.common.SerializerType;
import org.example.service.CalcService;
import org.example.transport.SocketTransport;
import org.example.transport.Transport;

import java.io.IOException;

public class ClientApp {
    public static void main(String[] args) throws IOException {
        Transport transport = new SocketTransport("localhost", 8080);
        RpcClient.init(transport, SerializerType.JSON);
        CalcService client = RpcClient.getProxy(CalcService.class);
        //System.out.println(client.add(2,9));
        System.out.println(client.divide(4.8,0.6));

        transport.close();
    }
}
