package com.remotesupport.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remotesupport.dto.SessionMessage;
import com.remotesupport.service.SessionService;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final Map<String, String> sessionIdBySocket = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public WebSocketHandler(ObjectMapper objectMapper, SessionService sessionService) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("Connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        SessionMessage payload = objectMapper.readValue(message.getPayload(), SessionMessage.class);

        switch (payload.getType()) {
            case "create-session" -> handleCreateSession(session);
            case "join-session" -> handleJoinSession(session, payload);
            case "offer" -> handleOffer(session, payload);
            case "answer" -> handleAnswer(session, payload);
            case "ice-candidate" -> handleIceCandidate(session, payload);
            case "control-request" -> handleControlRequest(session, payload);
            case "control-response" -> handleControlResponse(session, payload);
            default -> logger.warn("Unknown message type: {}", payload.getType());
        }
    }

    private void handleCreateSession(WebSocketSession session) throws IOException {
        String sessionId = generateSessionId();
        sessionService.createSession(sessionId, session);
        sessionIdBySocket.put(session.getId(), sessionId);
        session.getAttributes().put("role", "sender");

        send(session, "session-created", Map.of("sessionId", sessionId));
        logger.info("Session created: {}", sessionId);
    }

    private void handleJoinSession(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        if (!sessionService.sessionExists(sessionId)) {
            send(session, "session-error", Map.of("message", "Session not found"));
            return;
        }

        sessionService.addReceiver(sessionId, session);
        sessionIdBySocket.put(session.getId(), sessionId);
        session.getAttributes().put("role", "receiver");

        send(session, "session-joined", Map.of("sessionId", sessionId));
        sendToSession(sessionId, "receiver-connected", Map.of());
        logger.info("Receiver joined: {}", sessionId);
    }

    private void handleOffer(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "offer", payload.getOffer());
        logger.info("Offer sent: {}", sessionId);
    }

    private void handleAnswer(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "answer", payload.getAnswer());
        logger.info("Answer sent: {}", sessionId);
    }

    private void handleIceCandidate(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "ice-candidate", payload.getCandidate());
        logger.info("ICE candidate sent: {}", sessionId);
    }

    private void handleControlRequest(WebSocketSession session, SessionMessage payload) throws IOException {
        if (!"mouse".equals(payload.getControlType()) && !"keyboard".equals(payload.getControlType())) {
            return;
        }
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "control-request", Map.of("controlType", payload.getControlType(), "receiverId", session.getId()));
        logger.info("{} control requested", payload.getControlType());
    }

    private void handleControlResponse(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "control-response", Map.of("controlType", payload.getControlType(), "allowed", payload.isAllowed()));
        logger.info("{}: {}", payload.getControlType(), payload.isAllowed() ? "allowed" : "denied");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = sessionIdBySocket.remove(session.getId());
        if (sessionId != null) {
            sessionService.removeSession(sessionId, session);
        }
        logger.info("Disconnected: {}", session.getId());
    }

    private void send(WebSocketSession session, String type, Object data) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", type, "data", data))));
    }

    private void sendToSession(String sessionId, String type, Object data) throws IOException {
        WebSocketSession target = sessionService.getSenderSession(sessionId);
        if (target != null && target.isOpen()) {
            send(target, type, data);
        }
    }

    private void sendToOther(String sessionId, WebSocketSession origin, String type, Object data) throws IOException {
        WebSocketSession target = sessionService.getOtherSession(sessionId, origin);
        if (target != null && target.isOpen()) {
            send(target, type, data);
        }
    }

    private String generateSessionId() {
        int number = random.nextInt(90000000) + 10000000;
        return String.valueOf(number);
    }
}
