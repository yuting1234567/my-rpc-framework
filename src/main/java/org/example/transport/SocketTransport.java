package org.example.transport;

import org.example.client.RpcClient;
import org.example.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class SocketTransport implements Transport{

    private static final Logger logger = LoggerFactory.getLogger(SocketTransport.class);

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public SocketTransport(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public synchronized Response send(Request request) throws Exception {

        long start  = System.currentTimeMillis();

        try{

            logger.info("发送 RPC 请求：{}#{}",
                        request.getServiceName(),
                        request.getMethodName());

            //序列化
            byte[] body = RpcClient.getSerializer().serialize(request);

            //写长度+数据
            out.writeInt(body.length);
            out.write(body);
            out.flush();

            //读响应
            int len = in.readInt();
            byte[] readBytes = new byte[len];
            in.readFully(readBytes);

            Response response = RpcClient.getSerializer().deserialize(readBytes, Response.class);

            long cost = System.currentTimeMillis() - start;

            logger.info("RPC网络调用完成：{}#{},耗时：{}ms",
                        request.getServiceName(),
                        request.getMethodName(),
                        cost);

            return response;

        }catch (Exception e){

            logger.error("RPC网络异常：{}#{}",
                         request.getServiceName(),
                         request.getMethodName(),
                         e);

            throw e;
        }
    }

    @Override
    public void close() {
        //关闭输入输出流
        try{
            socket.close();
        }catch(IOException ignored){}
    }
}
