package io.best.tool.netty.string;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NettyStringServerHandler extends ChannelInboundHandlerAdapter {

    private static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    //保存客户端channel set
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @PostConstruct
    public void init(){
        timer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                channels.writeAndFlush(Unpooled.copiedBuffer("服务端 channel is active".getBytes(StandardCharsets.UTF_8)));
            }
        }, 1000, 3000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //保存客户端的链接
        channels.add(ctx.channel());
    }

    //接受到客户端的数据
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("客户端发来的数据：" + buf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.copiedBuffer("服务端say hi".getBytes(StandardCharsets.UTF_8)));
    }

    //服务端监听到客户端不活动
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        System.out.println("服务器:" + channel.id().asLongText() + "掉线");
        channels.remove(channel);
        System.out.println(channels.size());
    }
}
