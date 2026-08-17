package com.remotesupport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionMessage {

    private String type;
    private String sessionId;
    private String controlType;
    private JsonNode offer;
    private JsonNode answer;
    private JsonNode candidate;
    private boolean allowed;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getControlType() {
        return controlType;
    }

    public void setControlType(String controlType) {
        this.controlType = controlType;
    }

    public JsonNode getOffer() {
        return offer;
    }

    public void setOffer(JsonNode offer) {
        this.offer = offer;
    }

    public JsonNode getAnswer() {
        return answer;
    }

    public void setAnswer(JsonNode answer) {
        this.answer = answer;
    }

    public JsonNode getCandidate() {
        return candidate;
    }

    public void setCandidate(JsonNode candidate) {
        this.candidate = candidate;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
}
