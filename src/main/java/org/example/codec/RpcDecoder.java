package org.example.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.example.common.ProtocolConstants;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.RpcMessage;
import org.example.common.Serializer;
import org.example.common.SerializerType;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        //走到这里,in一定是一条完整的消息（上一个 handler 保证的——LengthFieldBasedFrameDecoder基于长度字段的分帧解码器）

        int magic = in.readInt();
        if(magic != ProtocolConstants.MAGIC) {
            ctx.close();
            throw new IllegalArgumentException("非法魔数：" + magic);
        }

        byte version = in.readByte();
        byte serCode = in.readByte();
        byte msgType = in.readByte();
        byte status = in.readByte();
        long requestId = in.readLong();
        int length = in.readInt();

        RpcMessage message = new RpcMessage();
        message.setMsgType(msgType);
        message.setSerializerCode(serCode);
        message.setStatus(status);
        message.setRequestId(requestId);

        if (length > 0) {
            byte[] bodyBytes = new byte[length];
            in.readBytes(bodyBytes);

            Serializer serializer = SerializerType.fromCode(serCode).getSerializer();

            if (msgType == ProtocolConstants.TYPE_REQUEST) {
                message.setBody(serializer.deserialize(bodyBytes, Request.class));
            }else if (msgType == ProtocolConstants.TYPE_RESPONSE) {
                message.setBody(serializer.deserialize(bodyBytes, Response.class));
            }
        }

        out.add(message);   //交给流水线上的下一个 Handler

    }
}
