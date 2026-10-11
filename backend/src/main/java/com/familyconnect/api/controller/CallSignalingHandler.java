package com.familyconnect.api.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallSignalingHandler extends TextWebSocketHandler {
    public static final String ROOM_ATTRIBUTE = "familyconnect.room";
    private static final Logger logger = LoggerFactory.getLogger(CallSignalingHandler.class);
    private static final int MAX_MESSAGE_LENGTH = 128_000;

    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public CallSignalingHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String room = (String) session.getAttributes().get(ROOM_ATTRIBUTE);
        if (room == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Set<WebSocketSession> participants = rooms.computeIfAbsent(room, ignored -> ConcurrentHashMap.newKeySet());
        synchronized (participants) {
            if (participants.size() >= 2) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            ArrayList<WebSocketSession> existing = new ArrayList<>(participants);
            participants.add(session);
            if (!existing.isEmpty()) {
                send(session, Map.of("type", "peer-present"));
                for (WebSocketSession participant : existing) {
                    send(participant, Map.of("type", "peer-joined"));
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (message.getPayloadLength() > MAX_MESSAGE_LENGTH) {
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(message.getPayload());
        } catch (JacksonException exception) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        JsonNode type = payload == null ? null : payload.get("type");
        if (type == null || !Set.of("offer", "answer", "candidate").contains(type.asText())) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String room = (String) session.getAttributes().get(ROOM_ATTRIBUTE);
        Set<WebSocketSession> participants = rooms.get(room);
        if (participants == null) return;
        synchronized (participants) {
            for (WebSocketSession participant : participants) {
                if (!participant.getId().equals(session.getId()) && participant.isOpen()) {
                    participant.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String room = (String) session.getAttributes().get(ROOM_ATTRIBUTE);
        if (room == null) return;
        Set<WebSocketSession> participants = rooms.get(room);
        if (participants == null) return;

        synchronized (participants) {
            if (!participants.remove(session)) return;
            if (participants.isEmpty()) {
                rooms.remove(room, participants);
                return;
            }
            for (WebSocketSession participant : participants) {
                if (participant.isOpen()) send(participant, Map.of("type", "peer-left"));
            }
        }
    }

    private void send(WebSocketSession session, Map<String, String> message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }
}
