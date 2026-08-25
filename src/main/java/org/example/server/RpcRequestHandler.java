package org.example.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.example.common.ProtocolConstants;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.RpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);

    private static final ExecutorService BIZ_POOL = new ThreadPoolExecutor(
            16, 64, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {

        if (msg.getMsgType() == ProtocolConstants.TYPE_HEARTBEAT) {
            RpcMessage pong = new RpcMessage();
            pong.setMsgType(ProtocolConstants.TYPE_HEARTBEAT);
            pong.setSerializerCode(msg.getSerializerCode());
            pong.setStatus((byte) 0);
            pong.setRequestId(msg.getRequestId());
            pong.setBody(null);
            ctx.writeAndFlush(pong);
            return;
        }

        if(msg.getMsgType() != ProtocolConstants.TYPE_REQUEST){
            return;    //心跳等其它类型，暂时不管
        }

        BIZ_POOL.execute(() -> handle(ctx, msg));
    }

    private void handle(ChannelHandlerContext ctx, RpcMessage msg) {

        Request request = (Request) msg.getBody();

        logger.debug("[收到请求]{}#{}",request.getServiceName(),request.getMethodName());

        Response response;
        try{
            Object result = Dispatcher.dispatch(request);
            response = Response.success(result);
        }catch (Exception e){
            response = Response.error(e.getClass().getSimpleName() + "：" + e.getMessage());
        }

        //造回信
        RpcMessage out = new RpcMessage();
        out.setMsgType(ProtocolConstants.TYPE_RESPONSE);
        out.setSerializerCode(msg.getSerializerCode());
        out.setRequestId(msg.getRequestId());
        out.setStatus(response.isSuccess() ? (byte) 0: (byte) 1);
        out.setBody(response);

        ctx.writeAndFlush(out);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event && event.state() == IdleState.READER_IDLE) {
            logger.warn("45秒未收到任何消息，关闭连接：{}", ctx.channel().remoteAddress());
            ctx.close();
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

