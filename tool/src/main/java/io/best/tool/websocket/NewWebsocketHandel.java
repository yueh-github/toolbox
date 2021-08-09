package io.best.tool.websocket;

import com.google.gson.Gson;
import io.best.tool.domain.PongMessageDomain;
import org.springframework.web.socket.*;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NewWebsocketHandel implements WebSocketHandler {

    //1，sub unsub

    //2，保存channel 根据不通的topic来订阅相关的消息

    //3，心跳策略

    //session,last time
    private Map<WebSocketSession, Long> timeMap = new ConcurrentHashMap<>();

    //topic ,session list
    private static Map<String, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        timeMap.put(webSocketSession, new Date().getTime());
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) throws Exception {

        //更新time
        timeMap.put(webSocketSession, System.currentTimeMillis());
        if (webSocketMessage instanceof TextMessage) {
            String messageBody = ((TextMessage) webSocketMessage).getPayload();

            if (messageBody.contains("ping")) {
                webSocketSession.sendMessage(new TextMessage(new Gson().toJson(new PongMessageDomain("pong", new Date().getTime()))));
                return;
            } else if (messageBody.contains("sub")) {
                //to sub

            } else if (messageBody.contains("unsub")) {
                //un sub
            }
            System.out.println("message body :" + messageBody);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {
        if (timeMap.containsKey(sessionMap)) {
            timeMap.remove(sessionMap);
            if (webSocketSession.isOpen()) {
                webSocketSession.close();
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
