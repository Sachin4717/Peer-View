package com.remotesupport.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remotesupport.dto.SessionMessage;
import com.remotesupport.service.SessionService;
import com.remotesupport.window.WindowHidingService;
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
    private final WindowHidingService windowHidingService;
    private final Map<String, String> sessionIdBySocket = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public WebSocketHandler(ObjectMapper objectMapper, SessionService sessionService,
            WindowHidingService windowHidingService) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
        this.windowHidingService = windowHidingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("Connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        SessionMessage payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), SessionMessage.class);
        } catch (JsonProcessingException e) {
            logger.warn("Malformed message from {}: {}", session.getId(), e.getMessage());
            send(session, "session-error", Map.of("message", "Malformed message"));
            return;
        }
        if (payload.getType() == null || payload.getType().isBlank()) {
            send(session, "session-error", Map.of("message", "Message type is required"));
            return;
        }

        switch (payload.getType()) {
            case "create-session" -> handleCreateSession(session);
            case "join-session" -> handleJoinSession(session, payload);
            case "offer" -> handleOffer(session, payload);
            case "answer" -> handleAnswer(session, payload);
            case "ice-candidate" -> handleIceCandidate(session, payload);
            case "audio-offer" -> handleAudioOffer(session, payload);
            case "audio-answer" -> handleAudioAnswer(session, payload);
            case "audio-ice-candidate" -> handleAudioIceCandidate(session, payload);
            case "control-request" -> handleControlRequest(session, payload);
            case "control-response" -> handleControlResponse(session, payload);
            case "control-event" -> handleControlEvent(session, payload);
            case "display-affinity", "display-affinity-result" -> handleDisplayAffinity(session, payload);
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
        if (sessionId == null || !sessionService.sessionExists(sessionId)) {
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
        if (!isRelayAllowed(session, payload, payload.getOffer())) {
            return;
        }
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "offer", payload.getOffer());
        logger.info("Offer sent: {}", sessionId);
    }

    private void handleAnswer(WebSocketSession session, SessionMessage payload) throws IOException {
        if (!isRelayAllowed(session, payload, payload.getAnswer())) {
            return;
        }
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "answer", payload.getAnswer());
        logger.info("Answer sent: {}", sessionId);
    }

    private void handleIceCandidate(WebSocketSession session, SessionMessage payload) throws IOException {
        if (!isRelayAllowed(session, payload, payload.getCandidate())) {
            return;
        }
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "ice-candidate", payload.getCandidate());
        logger.info("ICE candidate sent: {}", sessionId);
    }

    private void handleAudioOffer(WebSocketSession session, SessionMessage payload) throws IOException {
        if (!isRelayAllowed(session, payload, payload.getOffer())) {
            return;
        }
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "audio-offer", payload.getOffer());
        logger.info("Audio offer sent: {}", sessionId);
    }

    private void handleAudioAnswer(WebSocketSession session, SessionMessage payload) throws IOException {
        if (!isRelayAllowed(session, payload, payload.getAnswer())) {
            return;
        }
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "audio-answer", payload.getAnswer());
        logger.info("Audio answer sent: {}", sessionId);
    }

    private void handleAudioIceCandidate(WebSocketSession session, SessionMessage payload) throws IOException {
        if (!isRelayAllowed(session, payload, payload.getCandidate())) {
            return;
        }
        String sessionId = payload.getSessionId();
        sendToOther(sessionId, session, "audio-ice-candidate", payload.getCandidate());
        logger.info("Audio ICE candidate sent: {}", sessionId);
    }

    /**
     * Validates a relay request: the sender must be a session participant and
     * must include a non-blank session id and a non-null payload.
     */
    private boolean isRelayAllowed(WebSocketSession session, SessionMessage payload, JsonNode body)
            throws IOException {
        String sessionId = payload.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            send(session, "session-error", Map.of("message", "Session ID is required"));
            return false;
        }
        if (body == null || body.isNull()) {
            send(session, "session-error", Map.of("message", "Payload is required"));
            return false;
        }
        if (!sessionService.isSessionParticipant(sessionId, session)) {
            send(session, "session-error", Map.of("message", "Not a session participant"));
            return false;
        }
        return true;
    }

    private void handleControlRequest(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        if (!isSessionParticipant(sessionId, session, "control-request")) {
            return;
        }
        if (!"mouse".equals(payload.getControlType()) && !"keyboard".equals(payload.getControlType())) {
            send(session, "session-error", Map.of("message", "controlType must be mouse or keyboard"));
            return;
        }
        sendToOther(sessionId, session, "control-request", Map.of("controlType", payload.getControlType(), "receiverId", session.getId()));
        logger.info("{} control requested: {}", payload.getControlType(), sessionId);
    }

    private void handleControlResponse(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        if (!isSessionParticipant(sessionId, session, "control-response")) {
            return;
        }
        if (!"mouse".equals(payload.getControlType()) && !"keyboard".equals(payload.getControlType())) {
            send(session, "session-error", Map.of("message", "controlType must be mouse or keyboard"));
            return;
        }
        sendToOther(sessionId, session, "control-response", Map.of("controlType", payload.getControlType(), "allowed", payload.isAllowed()));
        logger.info("{} control {}: {}", payload.getControlType(), sessionId, payload.isAllowed() ? "allowed" : "denied");
    }

    /**
     * Relays individual mouse/keyboard events for desktop clients that inject
     * input at the OS level. Browser participants exchange these events over a
     * WebRTC data channel instead, so this path is only a fallback.
     */
    private void handleControlEvent(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        if (!isSessionParticipant(sessionId, session, "control-event")) {
            return;
        }
        if (payload.getEvent() == null || payload.getEvent().isNull()) {
            send(session, "session-error", Map.of("message", "Event payload is required"));
            return;
        }
        sendToOther(sessionId, session, "control-event", payload.getEvent());
        logger.debug("Control event relayed: {}", sessionId);
    }

    private boolean isSessionParticipant(String sessionId, WebSocketSession session, String messageType) throws IOException {
        if (sessionId == null || sessionId.isBlank()) {
            send(session, "session-error", Map.of("message", "Session ID is required"));
            return false;
        }
        if (!sessionService.isSessionParticipant(sessionId, session)) {
            send(session, "session-error", Map.of("message", "Not a session participant"));
            return false;
        }
        return true;
    }

    private void handleDisplayAffinity(WebSocketSession session, SessionMessage payload) throws IOException {
        String sessionId = payload.getSessionId();
        if (!sessionService.isSessionParticipant(sessionId, session)) {
            send(session, "session-error", Map.of("message", "Not a session participant"));
            return;
        }
        if (!payload.hasSupportedWindowDisplayAffinity()) {
            send(session, "session-error", Map.of("message", "Unsupported window display affinity"));
            return;
        }

        // When capture exclusion is requested, apply it to this process's own
        // windows (hides them from Chrome tab sharing, Teams, OBS, ...) and
        // report the real result instead of blindly forwarding false.
        boolean captureExcluded = payload.isCaptureExcluded();
        if (payload.isCaptureExclusionRequested()) {
            var hideResult = windowHidingService.hide(true);
            captureExcluded = Boolean.TRUE.equals(hideResult.get("captureExcluded"));
        }

        sendToOther(sessionId, session, payload.getType(), Map.of(
                "windowDisplayAffinity", SessionMessage.WDA_EXCLUDEFROMCAPTURE,
                "captureExclusionRequested", payload.isCaptureExclusionRequested(),
                "captureExcluded", captureExcluded));
        logger.info("Display affinity message relayed: {}", sessionId);
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
