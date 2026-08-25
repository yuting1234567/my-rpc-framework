package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.example.codec.RpcDecoder;
import org.example.codec.RpcEncoder;
import org.example.registry.Registry;
import org.example.registry.ZkRegistry;
import org.example.service.CalcService;
import org.example.service.CalcServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    public static void main(String[] args) throws Exception {

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        //装配服务
        Dispatcher.register(CalcService.class, new CalcServiceImpl());

        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try{
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline()
                              .addLast(new IdleStateHandler(45, 0, 0, TimeUnit.SECONDS))
                              .addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 16, 4, 0, 0))
                              .addLast(new RpcDecoder())
                              .addLast(new RpcEncoder())
                              .addLast(new RpcRequestHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("Netty Server 启动，监听 {}",port);


            //端口绑定成功之后，才注册到 ZK
            Registry registry = new ZkRegistry("localhost:2181");
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
            for (String serviceName : Dispatcher.getServiceNames()) {
                registry.register(serviceName, address);
            }

            future.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
