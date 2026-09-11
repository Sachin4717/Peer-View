package com.remotesupport.model;

import org.springframework.web.socket.WebSocketSession;

public class RemoteSession {

    private final String sessionId;
    private WebSocketSession senderSession;
    private WebSocketSession receiverSession;

    public RemoteSession(String sessionId, WebSocketSession senderSession) {
        this.sessionId = sessionId;
        this.senderSession = senderSession;
    }

    public String getSessionId() {
        return sessionId;
    }

    public WebSocketSession getSenderSession() {
        return senderSession;
    }

    public void setSenderSession(WebSocketSession senderSession) {
        this.senderSession = senderSession;
    }

    public WebSocketSession getReceiverSession() {
        return receiverSession;
    }

    public void setReceiverSession(WebSocketSession receiverSession) {
        this.receiverSession = receiverSession;
    }
}
