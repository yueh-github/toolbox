package io.best.tool.ws;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.best.tool.uitl.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.*;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.*;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Slf4j
public class ContractWebSocketHandel implements WebSocketHandler {

    @Resource
    private ApplicationContext applicationContext;

    @Value("${test.hbFuturesWsApiHost:wss://api.hbdm.vn}")
    private String hbFuturesWsHost;

    private static final String PONG_MSG_KEY = "pong";

    private static final Integer SESSION_TIMEOUT = 20 * 1000;

    private String module;

    //session 和最后一次的pong time
    private ConcurrentHashMap<WebSocketSession, Long> clientSessionMap = new ConcurrentHashMap<>(5000); //session -> 上一次pong的时间
    //topic 和订阅的session
    private ConcurrentHashMap<String, Set<WebSocketSession>> clientTopicSessionMap = new ConcurrentHashMap<>(5000);  //topic -> Set<session>


    //hb websocket time
    private ConcurrentHashMap<WebSocket, Long> huobiServerSocketMap = new ConcurrentHashMap<>(1000);
    //hb topic websocket
    private ConcurrentHashMap<String, WebSocket> huobiServerTopicSocketMap = new ConcurrentHashMap<>(5000);

    private ConcurrentHashMap<WebSocket, AtomicInteger> huobiServerReqSocketMap = new ConcurrentHashMap<>(10); //socket leftTimes

    private ConcurrentHashMap<String, WebSocketSession> reqSessionMap = new ConcurrentHashMap<>(5000); //req-id -> Session


    public ContractWebSocketHandel(String module) {
        this.module = module;
    }


    @Scheduled(cron = "0/5 * * * * ?")
    public void pingSession() {
        clientSessionMap.keySet().parallelStream()
                .forEach(webSocketSession -> {
                    if (webSocketSession != null && webSocketSession.isOpen()) {
                        pingToClient(webSocketSession);
                    }
                });
    }

    private void pingToClient(WebSocketSession session) {
        JsonObject pingData = new JsonObject();
        pingData.addProperty("ping", System.currentTimeMillis());
        sendMessage(session, JsonUtil.defaultGson().toJson(pingData));
        log.info("ping to client");
    }


    @Async
    @Scheduled(cron = "0/16 * * * * ?")
    public void killSession() {
        Set<WebSocketSession> sessions = clientSessionMap.keySet().stream().collect(Collectors.toSet());
        log.info("module:{} session number:{} Thread {}", module, sessions.size(), Thread.currentThread().getName());
        if (CollectionUtils.isEmpty(sessions)) {
            sessions = new HashSet<>();
        }
        sessions.forEach(webSocketSession -> {
            String sessionId = webSocketSession.getId();
            long lastActiveTime = clientSessionMap.get(webSocketSession);
            if (System.currentTimeMillis() - lastActiveTime > SESSION_TIMEOUT && webSocketSession != null && webSocketSession.isOpen()) {
                try {
                    webSocketSession.close();
                    clientSessionMap.remove(webSocketSession);
                    webSocketSession = null;
                    log.info("kill no heartbeat module:{} session(sessionId:{})", module, sessionId);
                    // now afterConnectionClosed will be invoked
                } catch (Exception e) {
                    log.warn("close inactive session {} failed", sessionId);
                }
            }
        });

        //检查火币服务端的socket是否断开
        Set<WebSocket> huobiServerSockets = huobiServerSocketMap.keySet().stream().collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(huobiServerSockets)) {
            huobiServerSockets = new HashSet<>();
        }
        log.info("module:{} huobiServerSockets:{} huobiServerTopicSockets:{}", module, huobiServerSockets.size(), huobiServerTopicSocketMap.size());
        huobiServerSockets.parallelStream().forEach(huobiServerSocket -> {
            long lastActiveTime = huobiServerSocketMap.get(huobiServerSocket);
            if (System.currentTimeMillis() - lastActiveTime > 20_000) {
                try {
                    huobiServerSocketMap.remove(huobiServerSocket);
                    if (huobiServerReqSocketMap.containsKey(huobiServerSocket)) {
                        huobiServerReqSocketMap.remove(huobiServerSocket);
                        log.info("module:{} huobi req socket expire now:{} aliveTime:{}", module, System.currentTimeMillis(), lastActiveTime);
                        return;
                    }
                    log.info("module:{} huobi socket expire now:{} aliveTime:{}", module, System.currentTimeMillis(), lastActiveTime);
                    // ReconnectHuobiSocket and send message
                    List<String> removedTopics = Lists.newArrayList();
                    for (String topic : huobiServerTopicSocketMap.keySet()) {
                        if (huobiServerTopicSocketMap.get(topic) == huobiServerSocket) {
                            removedTopics.add(topic);
                        }
                    }

                    removedTopics.forEach(topic -> { //从huobiServerTopicSocketMap摘除掉要删除的websocket
                        huobiServerTopicSocketMap.remove(topic);
                        applicationContext.publishEvent(TopicInvalidatedEvent.builder().topic(topic).build());
                    });

                    removedTopics.forEach(topic -> { //重新订阅
                        JsonObject jsonObject = new JsonObject();
                        if (module.equals("center-notification")) {
                            jsonObject.addProperty("op", "sub");
                            jsonObject.addProperty("topic", topic);
                            jsonObject.addProperty("cid", UUID.randomUUID().toString());
                        } else {
                            jsonObject.addProperty("sub", topic);
                            jsonObject.addProperty("id", UUID.randomUUID().toString());
                            if (topic.endsWith("high_freq") && topic.startsWith("market.") && topic.contains("depth")) {
                                jsonObject.addProperty("data_type", "incremental");
                            }
                        }
                        JsonElement json = JsonUtil.defaultJsonParser().parseString(JsonUtil.defaultGson().toJson(jsonObject));
                        Pair<TopicKeyEnum, String> topicPair = getTopic(json, "topic", "sub");
                        huobiWebsocketConnect(json, topicPair);
                        log.info("resend topic:{} to huobi server", topic);
                    });

                    huobiServerSocket.close(1001, "alive time overtime");
                    huobiServerSocket = null;
                } catch (Exception e) {
                    log.error("close huobi websocket error", e);
                } finally {

                }

            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        clientSessionMap.put(session, System.currentTimeMillis());
    }


    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        clientSessionMap.put(session, System.currentTimeMillis());
        if (message instanceof TextMessage) {
            String messageBody = ((TextMessage) message).getPayload();
            if (Strings.isNullOrEmpty(messageBody)) {
                return;
            }
            log.info("handleMessage : {} messageBody:{}", session.getId(), messageBody);
            JsonElement json = JsonParser.parseString(messageBody);
            if (json.isJsonNull() || json.isJsonPrimitive()) {
                return;
            }
            if (messageBody.contains(PONG_MSG_KEY)) {
                return;
            }

            Pair<TopicKeyEnum, String> topicPair = getTopic(json, "req", "unsub", "sub", "topic");
            if (topicPair == null) {
                log.error("not found topic in {} {}", "unsub,sub,topic,req", messageBody);
                return;
            }
            String requestTopic = topicPair.getRight();
            if (topicPair.getLeft() == TopicKeyEnum.UNSUB) { //unsub topic
                clientTopicSessionMap.getOrDefault(requestTopic, new HashSet<>()).remove(session);
                log.info("after session:{} unsub:{} topics:{}", session.getId(), requestTopic,
                        clientTopicSessionMap.getOrDefault(requestTopic, new HashSet<>()).stream().map(s -> s.getId()).collect(Collectors.toList()));
                return;
            }

            //todo
//            if (!clientTopicSessionMap.getOrDefault(requestTopic, new HashSet<>()).contains(session)) {//此session没有订阅过此topic，也就topic第一次被订阅
//                if (module.equals("center-notification")) {
//                    if (estimatedRecoveryTimeMap.containsKey(requestTopic) && estimatedRecoveryTimeMap.getOrDefault(requestTopic, 0L) > 0) {
//                        String res = "{\"op\":\"notify\",\"topic\":\"public.linear-swap.heartbeat\",\"event\":\"init\",\"ts\":#now,\"data\":{\"heartbeat\":0,\"estimated_recovery_time\":#estimated_recovery_time}}";
//                        res = res.replace("#now", System.currentTimeMillis() + "").replace("#estimated_recovery_time", estimatedRecoveryTimeMap.get(requestTopic).toString());
//                        sendMessage(session, res);
//                    }
//                }
//            }
//            if (!requestTopic.startsWith("market.") && !messageBody.contains(".heartbeat") && !messageBody.contains(".contract_info")) {
//                log.warn("error requestBody : {}", messageBody);
//            } else {
            try {
                if (topicPair.getLeft() != TopicKeyEnum.REQ) {
                    Set<WebSocketSession> sessions = clientTopicSessionMap.getOrDefault(requestTopic, new HashSet<>());
                    sessions.add(session);
                    clientTopicSessionMap.put(requestTopic, sessions);
                    huobiWebsocketConnect(json, topicPair);
                }
            } catch (Exception e) {
                log.warn("handle websocket message:{} from user has error", json, e);
            }
        }
//        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        log.warn("handleTransportError sessionId:{}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        if (clientSessionMap.containsKey(session)) {
            clientSessionMap.remove(session);
            log.debug("websocket connection(sessionId:{}) closed......", session.getId());
            if (session.isOpen()) {
                session.close();
            }
            session = null;
        } else {
            log.warn("{} no such websocket", session.getId());
            session = null;
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * @param json
     * @param keys
     * @return left-key right-topic
     */
    private Pair<TopicKeyEnum, String> getTopic(JsonElement json, String... keys) {
        for (String key : keys) {
            String topic = JsonUtil.getString(json, "." + key, "");
            if (!Strings.isNullOrEmpty(topic)) {
                return Pair.of(TopicKeyEnum.fromValue(key), topic);
            }
        }
        return null;
    }

    public void huobiWebsocketConnect(JsonElement json, Pair<TopicKeyEnum, String> topicPair) {
        String requestTopic = topicPair.getRight();
        if (Strings.isNullOrEmpty(requestTopic)) {
            return;
        }

        Pair<Boolean, WebSocket> huobiTopicWebsocketPair = getHuobiWebsocket(requestTopic);
        WebSocket webSocket;
        if (huobiTopicWebsocketPair.getRight() != null) {
            webSocket = huobiTopicWebsocketPair.getRight();
        } else { //第一次访问 要新建
            OkHttpClient proxyOkHttpClient = new OkHttpClient.Builder()
                    //.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.6.28.197", 7893)))
                    .build();
            Request request = new Request.Builder()
                    .url(hbFuturesWsHost + "/" + module).build();
            webSocket = proxyOkHttpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    log.info("WebSocket:{} onClosed code:{} reason:{}", webSocket.toString(), code, reason);
                    super.onClosed(webSocket, code, reason);
                }

                @Override
                public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    log.info("WebSocket:{} onClosing code:{} reason:{}", webSocket.toString(), code, reason);
                    super.onClosing(webSocket, code, reason);
                }

                @Override
                public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                    log.info("WebSocket:{} onFailure body:{}", webSocket.toString(), response);
                    super.onFailure(webSocket, t, response);
                }

                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                    log.info("received TextMessage:{}", text);
                    super.onMessage(webSocket, text);
                }

                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                    try {
                        huobiServerSocketMap.put(webSocket, System.currentTimeMillis());
                        bytes.asByteBuffer();
                        String response = decompress(bytes.toByteArray());
                        super.onMessage(webSocket, bytes);
                        log.info("received BinaryMessage, module:{}, data:{}", module, response.length() < 200 ? response : response.substring(0, 199));
                        if (response.contains("\"ping\"")) {
                            webSocket.send(response.replace("ping", "pong"));
                            return;
                        }
                        onHuobiMessageReceive(response);
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }

                @Override
                public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                    super.onOpen(webSocket, response);
                    log.info("on open : {}", json.toString());
                    webSocket.send(json.toString());
                }
            });
            huobiServerSocketMap.put(webSocket, System.currentTimeMillis());
            huobiServerTopicSocketMap.putIfAbsent(requestTopic, webSocket);
            log.info("topic:{} new huobiServerSocket", requestTopic);
        }

        if (!huobiTopicWebsocketPair.getLeft() && huobiTopicWebsocketPair.getRight() != null) { //新topic复用旧通道 直接发送
            log.info("topic:{} reuse huobiServerSocket", requestTopic);
            webSocket.send(json.toString());
            huobiServerTopicSocketMap.putIfAbsent(requestTopic, webSocket);
        } else {
            //topic 已经有websocket为其服务了
        }
    }

    //boolean - topic是否订阅过  websocket是否建立过通道
    private Pair<Boolean, WebSocket> getHuobiWebsocket(String topic) {
        boolean containsTopic = huobiServerTopicSocketMap.containsKey(topic);
        if (containsTopic) {
            return Pair.of(true, huobiServerTopicSocketMap.get(topic));
        }
        Map<WebSocket, Integer> counter = Maps.newHashMap();
        for (String t : huobiServerTopicSocketMap.keySet()) {
            WebSocket webSocket = huobiServerTopicSocketMap.get(t);
            int times = counter.getOrDefault(webSocket, 0);
            counter.put(webSocket, times + 1);
        }

        for (WebSocket webSocket : counter.keySet()) { //每个火币socket保持50个topic的订阅
            if (counter.get(webSocket) < 50) {
                return Pair.of(false, webSocket);
            }
        }
        return Pair.of(false, null);
    }

    void sendMessage(WebSocketSession session, String json) {
        if (Strings.isNullOrEmpty(json)) {
            return;
        }
        try {
            if (session != null && session.isOpen()) {
                synchronized (session) {
                    //压缩
                    //session.sendMessage(new BinaryMessage(compress(json)));
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            if (!session.isOpen()) {
                log.warn("send WS message(topic:{}), but session was closed", json.length() > 200 ? json.substring(0, 198) : json);
            } else {
                log.error("send WS message(topic:{}) IoException", json, e);
            }
        } catch (Exception e) {
            if (e instanceof IllegalStateException
                    || (e.getCause() != null && e.getCause() instanceof IllegalStateException)) {
                log.warn("send WS message(topic:{}) IllegalStateException", json.length() > 200 ? json.substring(0, 198) : json, e);
            } else {
                log.error("send WS message(topic:{}) Exception", json.length() > 200 ? json.substring(0, 198) : json, e);
            }
        }
    }

    private String decompress(byte[] depressData) {
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
            log.warn("decompress data error");
            return "error";
        }
    }

    private void onHuobiMessageReceive(String response) {
        JsonElement json = JsonParser.parseString(response);
        Pair<TopicKeyEnum, String> topicPair = getTopic(json, "ch", "topic", "rep");
        if (topicPair == null) {
            return;
        }

        if (topicPair.getLeft() != TopicKeyEnum.REQ) {
            Set<WebSocketSession> sessions = clientTopicSessionMap
                    .getOrDefault(topicPair.getRight(), new HashSet<>()).stream().collect(Collectors.toSet());
            sessions.parallelStream().forEach(webSocketSession -> {
                try {
                    sendMessage(webSocketSession, response);
                } catch (Exception e) {
                    log.error("sendMessage topic:{} error", topicPair.getRight(), e);
                }
            });
        } else { //req请求
            String reqId = JsonUtil.getString(json, ".id", "");
            WebSocketSession webSocketSession = reqSessionMap.get(reqId);
            if (webSocketSession == null) {
                return;
            }
            log.info("req webSocketSession : {}", webSocketSession.getTextMessageSizeLimit());
            sendMessage(webSocketSession, response);
            reqSessionMap.remove(reqId);
        }
    }
}
