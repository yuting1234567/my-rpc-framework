package org.example.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.example.common.ProtocolConstants;
import org.example.common.RpcMessage;
import org.example.common.Serializer;
import org.example.common.SerializerType;

public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) {

        //写头，顺序必须和解码器读的顺序完全一致
        out.writeInt(ProtocolConstants.MAGIC);
        out.writeByte(ProtocolConstants.VERSION);
        out.writeByte(msg.getSerializerCode());
        out.writeByte(msg.getMsgType());
        out.writeByte(msg.getStatus());
        out.writeLong(msg.getRequestId());

        Object body = msg.getBody();

        if (body == null) {         //心跳包，没有 body
            out.writeInt(0);
            return;
        }

        //先序列化，才知道长度
        Serializer serializer = SerializerType.fromCode(msg.getSerializerCode()).getSerializer();
        byte[] bodyBytes = serializer.serialize(body);

        out.writeInt(bodyBytes.length);
        out.writeBytes(bodyBytes);
    }
}
