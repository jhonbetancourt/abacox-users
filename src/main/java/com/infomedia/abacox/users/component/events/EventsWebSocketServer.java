package com.infomedia.abacox.users.component.events;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableWebSocket
@Log4j2
@RequiredArgsConstructor
public class EventsWebSocketServer extends TextWebSocketHandler implements WebSocketConfigurer {

    private final ObjectMapper objectMapper;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(this, "/websocket/module").setAllowedOrigins("*");
    }

    private static final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected " + session.getRemoteAddress());
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Disconnected " + session.getRemoteAddress());
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("Received message from " + session.getRemoteAddress() + ": " + message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Error occurred on " + session.getRemoteAddress() + ": " + exception.getMessage(), exception);
        sessions.remove(session);
    }

    public void sendEventMessage(String source, EventType eventType, String content) {
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new EventMessage(source, eventType, content))));
            } catch (IOException e) {
                log.error("Error occurred while sending message to " + session.getRemoteAddress() + ": " + e.getMessage(), e);
            }
        }
    }
}
