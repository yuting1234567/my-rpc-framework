package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.example.codec.RpcDecoder;
import org.example.codec.RpcEncoder;
import org.example.common.ProtocolConstants;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.RpcMessage;
import org.example.common.SerializerType;
import org.example.server.RpcRequestHandler;

public class ServerPipelineTest {
    public static void main(String[] args) {

        //先用编码器造出“客户端会发出的字节”
        Request request = new Request(
                "org.example.service.CalcService",
                "add",
                new Class[]{int.class, int.class},
                new Object[]{2, 9}
        );

        RpcMessage requestMsg = new RpcMessage();
        requestMsg.setMsgType(ProtocolConstants.TYPE_REQUEST);
        requestMsg.setSerializerCode(SerializerType.JSON.getCode());
        requestMsg.setStatus((byte) 0);
        requestMsg.setRequestId(7L);
        requestMsg.setBody(request);

        EmbeddedChannel fakeClient = new EmbeddedChannel(new RpcEncoder());
        fakeClient.writeOutbound(requestMsg);
        ByteBuf requestBytes = fakeClient.readOutbound();

        //喂给服务端流水线
        EmbeddedChannel server = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(1024 * 1024, 16, 4, 0, 0),
                new RpcDecoder(),
                new RpcEncoder(),
                new RpcRequestHandler()
        );

        server.writeInbound(requestBytes);

        //把服务端吐出来的响应字节解开看看
        ByteBuf responseBytes = server.readOutbound();

        EmbeddedChannel decoder = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(1024 * 1024, 16, 4, 0, 0),
                new RpcDecoder()
        );
        decoder.writeInbound(responseBytes);

        RpcMessage responseMsg = decoder.readInbound();
        Response response = (Response) responseMsg.getBody();

        System.out.println("requestId = " + responseMsg.getRequestId());
        System.out.println("结果 = " + response.getData());

    }
}
