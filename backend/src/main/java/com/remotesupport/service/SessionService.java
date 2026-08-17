package com.remotesupport.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
public class SessionService {

    private final Map<String, WebSocketSession> senders = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> receivers = new ConcurrentHashMap<>();

    public void createSession(String sessionId, WebSocketSession session) {
        senders.put(sessionId, session);
    }

    public boolean sessionExists(String sessionId) {
        return senders.containsKey(sessionId);
    }

    public void addReceiver(String sessionId, WebSocketSession session) {
        receivers.put(sessionId, session);
    }

    public WebSocketSession getSenderSession(String sessionId) {
        return senders.get(sessionId);
    }

    public WebSocketSession getReceiverSession(String sessionId) {
        return receivers.get(sessionId);
    }

    public WebSocketSession getOtherSession(String sessionId, WebSocketSession origin) {
        WebSocketSession sender = getSenderSession(sessionId);
        WebSocketSession receiver = getReceiverSession(sessionId);
        if (origin != null && origin.getId().equals(sender != null ? sender.getId() : null)) {
            return receiver;
        }
        return sender;
    }

    public void removeSession(String sessionId, WebSocketSession session) {
        WebSocketSession sender = senders.get(sessionId);
        WebSocketSession receiver = receivers.get(sessionId);
        if (sender != null && sender.getId().equals(session.getId())) {
            senders.remove(sessionId);
        }
        if (receiver != null && receiver.getId().equals(session.getId())) {
            receivers.remove(sessionId);
        }
    }
}
