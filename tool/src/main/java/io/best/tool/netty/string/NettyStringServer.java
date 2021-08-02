package io.best.tool.netty.string;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyStringServer {


    public static void main(String[] args) throws Exception {

        //boss线程组 1个线程负责接受客户端的链接
        NioEventLoopGroup boosGroup = new NioEventLoopGroup(1);

        //工作线程读 负责处理链接的io事件 默认CUP核数 * 2
        NioEventLoopGroup workGroup = new NioEventLoopGroup();

        //服务端启动配置
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        try {
            //线程组配置
            serverBootstrap.group(boosGroup, workGroup);
            //异步NIO
            serverBootstrap.channel(NioServerSocketChannel.class);

            serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);

            //核心处理
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) throws Exception {
                    channel.pipeline().addLast(new NettyStringServerHandler());
                }
            });

            ChannelFuture channelFuture = serverBootstrap.bind(9090).sync().await();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            boosGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
