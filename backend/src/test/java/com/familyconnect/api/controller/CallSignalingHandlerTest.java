package com.familyconnect.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class CallSignalingHandlerTest {
    private final CallSignalingHandler handler = new CallSignalingHandler(new ObjectMapper());

    @Test
    void connectsTwoParticipantsAndRelaysCallSetup() throws Exception {
        WebSocketSession first = session("first");
        WebSocketSession second = session("second");

        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        verify(first).sendMessage(any(TextMessage.class));
        verify(second).sendMessage(any(TextMessage.class));

        TextMessage offer = new TextMessage("{\"type\":\"offer\",\"description\":{\"type\":\"offer\"}}");
        handler.handleTextMessage(first, offer);
        ArgumentCaptor<TextMessage> secondMessages = ArgumentCaptor.forClass(TextMessage.class);
        verify(second, times(2)).sendMessage(secondMessages.capture());
        assertEquals("{\"type\":\"peer-present\"}", secondMessages.getAllValues().get(0).getPayload());
        assertEquals(offer.getPayload(), secondMessages.getAllValues().get(1).getPayload());

        handler.afterConnectionClosed(second, CloseStatus.NORMAL);
        ArgumentCaptor<TextMessage> firstMessages = ArgumentCaptor.forClass(TextMessage.class);
        verify(first, times(2)).sendMessage(firstMessages.capture());
        assertEquals("{\"type\":\"peer-left\"}", firstMessages.getAllValues().get(1).getPayload());
    }

    @Test
    void refusesThirdParticipantWithoutNotifyingTheCall() throws Exception {
        WebSocketSession first = session("first");
        WebSocketSession second = session("second");
        WebSocketSession third = session("third");

        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        handler.afterConnectionEstablished(third);

        verify(third).close(CloseStatus.POLICY_VIOLATION);
        verify(first, times(1)).sendMessage(any(TextMessage.class));
        verify(second, times(1)).sendMessage(any(TextMessage.class));
        handler.afterConnectionClosed(third, CloseStatus.POLICY_VIOLATION);
        verify(first, times(1)).sendMessage(any(TextMessage.class));
        verify(second, times(1)).sendMessage(any(TextMessage.class));
    }

    private WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(new HashMap<>(java.util.Map.of(
            CallSignalingHandler.ROOM_ATTRIBUTE,
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"
        )));
        return session;
    }
}
