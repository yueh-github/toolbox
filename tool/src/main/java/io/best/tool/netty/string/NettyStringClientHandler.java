package io.best.tool.netty.string;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;


public class NettyStringClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("成功链接:" + ctx.channel().id().asShortText());
        ctx.writeAndFlush(Unpooled.copiedBuffer("IM:" + ctx.channel().id().asShortText(), CharsetUtil.UTF_8));
    }

    //接受到客户端的数据
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("接受到服务端的返回消息：" + buf.toString(CharsetUtil.UTF_8));
        ctx.writeAndFlush(Unpooled.copiedBuffer(ctx.channel().id().asLongText() + ":已接收", CharsetUtil.UTF_8));
    }
}
