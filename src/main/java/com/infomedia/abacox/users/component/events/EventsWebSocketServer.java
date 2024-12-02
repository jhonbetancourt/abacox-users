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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private WebSocketSession session = null;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected " + session.getRemoteAddress());
        this.session = session;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Disconnected " + session.getRemoteAddress());
        this.session = null;
    }

    private final Map<UUID, CompletableFuture<ResponseMessage>> pendingRequests = new ConcurrentHashMap<>();
    private static final long REQUEST_TIMEOUT_SECONDS = 30;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("Received message from " + session.getRemoteAddress() + ": " + message.getPayload());

        WSMessage wsMessage = null;
        try {
            wsMessage = objectMapper.readValue(message.getPayload(), WSMessage.class);
        } catch (Exception e) {
            log.error("Error processing message: " + e.getMessage(), e);
        }

        if(wsMessage!=null&&wsMessage.getMessagetype().equals(MessageType.RESPONSE)){
            try {
                // Try to parse the message as a response
                ResponseMessage response = objectMapper.readValue(message.getPayload(), ResponseMessage.class);
                CompletableFuture<ResponseMessage> future = pendingRequests.remove(response.getId());

                if (future != null) {
                    future.complete(response);
                }
            } catch (Exception e) {
                log.error("Error processing response: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Error occurred on " + session.getRemoteAddress() + ": " + exception.getMessage(), exception);
    }

    public void sendEventMessage(String source, EventType eventType, String content) {
        try {
            WSMessage message = EventMessage.builder()
                    .source(source)
                    .eventType(eventType)
                    .content(content)
                    .id(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .messagetype(MessageType.EVENT)
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error("Error occurred while sending message to " + session.getRemoteAddress() + ": " + e.getMessage(), e);
        }
    }

    public ResponseMessage sendRequestMessageAndAwaitResponse(String source, String service, String function, Map<String, Object> arguments)
            throws IOException, TimeoutException {
        return sendRequestMessageAndAwaitResponse(source, service, function, arguments, REQUEST_TIMEOUT_SECONDS);
    }

    public ResponseMessage sendRequestMessageAndAwaitResponse(String source, String service, String function, Map<String, Object> arguments, long timeoutSeconds)
            throws IOException, TimeoutException {

        if (session == null || !session.isOpen()) {
            throw new IOException("WebSocket session is not available");
        }

        WSMessage requestMessage = RequestMessage.builder()
                .source(source)
                .function(function)
                .arguments(arguments)
                .service(service)
                .id(UUID.randomUUID())
                .timestamp(Instant.now())
                .messagetype(MessageType.REQUEST)
                .build();

        // Create a CompletableFuture to handle the async response
        CompletableFuture<ResponseMessage> responseFuture = new CompletableFuture<>();
        pendingRequests.put(requestMessage.getId(), responseFuture);

        try {
            // Send the request
            String requestJson = objectMapper.writeValueAsString(requestMessage);
            session.sendMessage(new TextMessage(requestJson));

            // Wait for the response with timeout
            return responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(requestMessage.getId());
            throw new TimeoutException("Request timed out after " + timeoutSeconds + " seconds");
        } catch (Exception e) {
            pendingRequests.remove(requestMessage.getId());
            throw new IOException("Error while sending request or receiving response", e);
        }
    }
}
