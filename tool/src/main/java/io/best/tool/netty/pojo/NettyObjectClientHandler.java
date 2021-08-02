package io.best.tool.netty.pojo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Arrays;

public class NettyObjectClientHandler extends ChannelInboundHandlerAdapter {

    private TransportObject getTransportObject() {
        TransportObject to = new TransportObject();
        to.setId(10001);
        to.setName("hao.yue");
        to.setUserList(Arrays.asList("11", "12", "13"));
        return to;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(getTransportObject());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
