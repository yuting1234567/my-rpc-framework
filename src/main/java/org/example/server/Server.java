package org.example.server;

import org.example.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static final Serializer serializer = SerializerType.JSON.getSerializer();
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        logger.info("RPC Server started on port 8080");

        while (true) {
            Socket socket = serverSocket.accept();
            pool.submit(() -> handle(socket));
        }
    }

    public static void handle(Socket socket){
        logger.info("收到客户端连接：{}", socket.getRemoteSocketAddress());
        try(
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())
        ){
            while (true){
                int len;
                try{
                    len = in.readInt();
                }catch (EOFException e){
                    logger.info("客户端关闭连接：{}", socket.getRemoteSocketAddress());
                    break;
                }
                byte[] data = new byte[len];
                in.readFully(data);

                //反序列化
                Request req = serializer.deserialize(data, Request.class);

                Response resp;
                try{
                    Object result = Dispatcher.dispatch(req);
                    resp = Response.success(result);
                }catch (Exception e){
                    resp = Response.error(e.getMessage());
                }

                byte[] respBytes = serializer.serialize(resp);
                out.writeInt(respBytes.length);
                out.write(respBytes);
                out.flush();
            }

        }catch (Exception e){
            logger.error("处理客户端请求异常：{}", socket.getRemoteSocketAddress(), e);
        }
    }

}
