package org.example.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.example.client.PendingRequests;
import org.example.client.RpcResponseHandler;
import org.example.codec.RpcDecoder;
import org.example.codec.RpcEncoder;
import org.example.common.ProtocolConstants;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.RpcBizException;
import org.example.common.RpcException;
import org.example.common.RpcMessage;
import org.example.common.SerializerType;
import org.example.loadbalance.LoadBalance;
import org.example.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NettyClientTransport implements Transport {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientTransport.class);

    private final Registry registry;
    private final LoadBalance loadBalance;

    private final EventLoopGroup group = new NioEventLoopGroup();
    private final Bootstrap bootstrap;
    private final SerializerType serializerType;

    //连接池：一个地址一条长连接
    private final Map<InetSocketAddress, Channel> channels = new ConcurrentHashMap<>();

    public NettyClientTransport(Registry registry, LoadBalance loadBalance, SerializerType serializerType) throws Exception {
        this.registry = registry;
        this.loadBalance = loadBalance;
        this.serializerType = serializerType;

        this.bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS))
                                .addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 16, 4, 0, 0))
                                .addLast(new RpcDecoder())
                                .addLast(new RpcEncoder())
                                .addLast(new RpcResponseHandler());
                    }
                });
    }

    @Override
    public Response send(Request request) throws Exception {

        Sent sent = doSend(request);

        try{
            //守着盒子等三秒
            return (Response) sent.future().get(3, TimeUnit.SECONDS).getBody();
        }catch (TimeoutException e){
            PendingRequests.remove(sent.id);   //超时就把盒子撤掉
            throw e;
        }
    }

    private Channel getChannel(InetSocketAddress address) throws Exception {
        Channel channel = channels.get(address);
        if (channel != null && channel.isActive()) {
            return channel;   //快路径，直接复用
        }

        synchronized (this) {
            channel = channels.get(address);
            if (channel != null && channel.isActive()) {
                return channel;  //双重检查
            }

            Channel newChannel = bootstrap.connect(address).sync().channel();
            channels.put(address, newChannel);
            logger.debug("[连接池]新建连接 -> {}", address);

            //连接断了自动从池子里摘掉
            newChannel.closeFuture().addListener(f -> {
                channels.remove(address, newChannel);
                logger.debug("[连接池] 连接断开，已移除 -> {}", address);
            });

            return newChannel;
        }
    }

    private record Sent(long id, CompletableFuture<RpcMessage> future) {}

    private Sent doSend(Request request) throws Exception {
        List<InetSocketAddress> addresses = registry.lookup(request.getServiceName());
        if (addresses.isEmpty()) {
            throw new RpcException("没有可用的服务提供者：" + request.getServiceName(), null);
        }

        InetSocketAddress target = loadBalance.select(addresses);
        Channel channel = getChannel(target);

        long id = PendingRequests.nextId();
        CompletableFuture<RpcMessage> future = PendingRequests.register(id);

        RpcMessage msg = new RpcMessage();
        msg.setMsgType(ProtocolConstants.TYPE_REQUEST);
        msg.setSerializerCode(serializerType.getCode());
        msg.setStatus((byte) 0);
        msg.setRequestId(id);
        msg.setBody(request);

        channel.writeAndFlush(msg);
        return new Sent(id, future);
    }

    @Override
    public CompletableFuture<Response> sendAsync(Request request) throws Exception {
        Sent sent = doSend(request);
        return sent.future().orTimeout(3, TimeUnit.SECONDS).whenComplete((m, e) -> {
            if(e != null) PendingRequests.remove(sent.id());
        }).thenApply(m -> (Response) m.getBody());

    }

    @Override
    public void close() {
        channels.values().forEach(Channel::close);
        group.shutdownGracefully();
        registry.close();
    }
}
