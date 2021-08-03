package io.best.tool.netty.websocket;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;

public class WebSocketClientDemoHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public WebSocketClientDemoHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handshaker.handshake(ctx.channel());
        Map<String, String> map = new HashMap<>();
        map.put("sub", "market.BTC-USDT.kline.1min");
        map.put("id", "id1");
        ctx.writeAndFlush(Unpooled.copiedBuffer(new Gson().toJson(map).getBytes()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("WebSocket Client disconnected!");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        Channel channel = channelHandlerContext.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(channel, (FullHttpResponse) o);
                System.out.println("WebSocket Client connected!");
                handshakeFuture.setSuccess();
            } catch (WebSocketHandshakeException ex) {
                System.out.println("WebSocket Client failed to connect!");
                handshakeFuture.setFailure(ex);
            }
            return;
        }

        if (o instanceof FullHttpResponse) {
            FullHttpResponse fullHttpResponse = (FullHttpResponse) o;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + fullHttpResponse.getStatus() +
                            ", content=" + fullHttpResponse.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame webSocketFrame = (WebSocketFrame) o;

        if (webSocketFrame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) webSocketFrame;
            System.out.println("WebSocket Client received message: " + textFrame.text());
        } else if (webSocketFrame instanceof PongWebSocketFrame) {
            System.out.println("WebSocket Client received pong");
        } else if (webSocketFrame instanceof CloseWebSocketFrame) {
            System.out.println("WebSocket Client received closing");
            channel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if(!handshakeFuture.isDone()){
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
