package com.familyconnect.api.config;

import com.familyconnect.api.controller.CallSignalingHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class CallSignalingConfig implements WebSocketConfigurer {
    private final CallSignalingHandler callSignalingHandler;
    private final String[] allowedOrigins;

    public CallSignalingConfig(
        CallSignalingHandler callSignalingHandler,
        @Value("${familyconnect.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String allowedOriginsConfig
    ) {
        this.callSignalingHandler = callSignalingHandler;
        this.allowedOrigins = Arrays.stream(allowedOriginsConfig.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callSignalingHandler, "/signal")
            .addInterceptors(roomCodeInterceptor())
            .setAllowedOrigins(allowedOrigins);
    }

    private HandshakeInterceptor roomCodeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                org.springframework.http.server.ServerHttpRequest request,
                org.springframework.http.server.ServerHttpResponse response,
                org.springframework.web.socket.WebSocketHandler handler,
                java.util.Map<String, Object> attributes
            ) {
                String query = request.getURI().getRawQuery();
                if (query == null) return false;
                for (String parameter : query.split("&")) {
                    String[] pair = parameter.split("=", 2);
                    if (pair.length == 2 && pair[0].equals("room") && pair[1].matches("[a-f0-9]{32}")) {
                        attributes.put(CallSignalingHandler.ROOM_ATTRIBUTE, pair[1]);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void afterHandshake(
                org.springframework.http.server.ServerHttpRequest request,
                org.springframework.http.server.ServerHttpResponse response,
                org.springframework.web.socket.WebSocketHandler handler,
                Exception exception
            ) {
            }
        };
    }
}
