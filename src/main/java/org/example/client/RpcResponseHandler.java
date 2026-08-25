package org.example.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.example.common.ProtocolConstants;
import org.example.common.RpcMessage;
import org.example.common.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final Logger logger = LoggerFactory.getLogger(RpcResponseHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {

        if (msg.getMsgType() != ProtocolConstants.TYPE_RESPONSE) {
            return;
        }
        PendingRequests.complete(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event && event.state() == IdleState.WRITER_IDLE) {
            RpcMessage ping = new RpcMessage();
            ping.setMsgType(ProtocolConstants.TYPE_HEARTBEAT);
            ping.setSerializerCode(SerializerType.JSON.getCode());
            ping.setStatus((byte) 0);
            ping.setRequestId(0L);
            ping.setBody(null);   //心跳没有 body

            ctx.writeAndFlush(ping);
            logger.debug("发送心跳 -> {}", ctx.channel().remoteAddress());
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
