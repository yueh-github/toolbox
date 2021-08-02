package io.best.tool.netty.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import okio.ByteString;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class OKhttpWebsocket {

    private static final OkHttpClient okHttpClient
            = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(100, 60, TimeUnit.SECONDS))
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5)).build();

    public static void main(String[] args) {
        Request request = new Request.Builder().url("https://api.btcgateway.pro/linear-swap-ws").build();
        WebSocket webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                System.out.println("WebSocket:{} onClosed code:{} reason:{}" + webSocket.toString());
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                System.out.println("WebSocket:{} onClosing code:{} reason:{}" + webSocket.toString());
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                System.out.println("WebSocket:{} onFailure body:{}" + webSocket.toString());
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                System.out.println("received TextMessage:{}" + text);
                super.onMessage(webSocket, text);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                try {
                    String message = decompress(bytes.toByteArray());
                    if (message.contains("ping")) {
                        Gson gson = new Gson();
                        Ping ping = gson.fromJson(message, Ping.class);
                        Map<String, Long> map = new HashMap<>();
                        map.put("pong", ping.getPing());
                        webSocket.send(new Gson().toJson(map));
                    }
                    System.out.println("call result :" + message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                super.onOpen(webSocket, response);
                Map<String, String> map = new HashMap<>();
                map.put("sub", "market.BTC-USDT.kline.1min");
                map.put("id", "id1");
                webSocket.send(new Gson().toJson(map));
            }
        });
    }


    private static String decompress(byte[] depressData) {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(depressData);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            GZIPInputStream gis = new GZIPInputStream(is);

            int count;
            byte data[] = new byte[1024];
            while ((count = gis.read(data, 0, 1024)) != -1) {
                os.write(data, 0, count);
            }
            gis.close();
            depressData = os.toByteArray();
            os.flush();
            os.close();
            is.close();
            return new String(depressData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
