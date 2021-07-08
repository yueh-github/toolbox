package io.best.tool;

import io.best.tool.ws.ContractWebSocketHandel;
import io.best.tool.ws.ContractWebSocketHandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;


@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {


    @Resource
    private ContractWebSocketHandshakeInterceptor contractWebSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {


        registry.addHandler(contractWebSocketHandel(), "/api/ws/contract-ws", "/mapi/ws/contract-ws")
                .setAllowedOrigins("*").addInterceptors(contractWebSocketHandshakeInterceptor);
    }

    @Bean
    public ContractWebSocketHandel contractWebSocketHandel() {
        return new ContractWebSocketHandel("linear-swap-ws");
    }
}
