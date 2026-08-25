package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.example.codec.RpcDecoder;
import org.example.codec.RpcEncoder;
import org.example.common.ProtocolConstants;
import org.example.common.Request;
import org.example.common.RpcMessage;
import org.example.common.SerializerType;

public class CodecTest {
    public static void main(String[] args) {
        //造一条消息
        Request request = new Request(
                "org.example.service.CalcService",
                "add",
                new Class[]{int.class,int.class},
                new Object[]{2,9}
        );

        RpcMessage msg = new RpcMessage();
        msg.setMsgType(ProtocolConstants.TYPE_REQUEST);
        msg.setSerializerCode(SerializerType.JSON.getCode());
        msg.setStatus((byte) 0);
        msg.setRequestId(7L);
        msg.setBody(request);

        //编码
        EmbeddedChannel encodeCh = new EmbeddedChannel(new RpcEncoder());
        encodeCh.writeOutbound(msg);
        ByteBuf buf = encodeCh.readOutbound();

        System.out.println("编码后共 " + buf.readableBytes() + " 字节");

        //解码
        EmbeddedChannel decodeCh = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(1024 * 1024, 16, 4, 0, 0),
                new RpcDecoder());

        decodeCh.writeInbound(buf);
        RpcMessage decoded = decodeCh.readInbound();

        System.out.println("requestId = " + decoded.getRequestId());
        Request r = (Request) decoded.getBody();
        System.out.println("解出来 = " + r.getServiceName() + "#" + r.getMethodName());
    }
}
