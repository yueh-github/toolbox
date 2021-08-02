package io.best.tool.netty.websocket;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<String> {

    private static ChannelGroup clientGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private WebSocketClientHandshaker webSocketClientHandshaker;

    private static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    public WebSocketClientHandler(WebSocketClientHandshaker webSocketClientHandshaker) {
        this.webSocketClientHandshaker = webSocketClientHandshaker;
        timer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Map<String, String> map = new HashMap<>();
                map.put("op", "pong");
                map.put("ts", String.valueOf(new Date().getTime()));
                clientGroup.writeAndFlush(Unpooled.copiedBuffer(new Gson().toJson(map).getBytes(StandardCharsets.UTF_8)));
            }
        }, 1000, 3000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelActive");
        webSocketClientHandshaker.handshake(ctx.channel());
        Map<String, String> map = new HashMap<>();
        map.put("sub", "market.BTC-USDT.kline.1min");
        map.put("id", "id1");
        ctx.writeAndFlush(Unpooled.copiedBuffer(new Gson().toJson(map).getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        clientGroup.add(channelHandlerContext.channel());
        System.out.println("channelRead0:" + s);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        clientGroup.remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clientGroup.remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        clientGroup.remove(ctx.channel());
        ctx.close();
    }
}
