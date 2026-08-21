package com.remotesupport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionMessage {

    /**
     * Windows WDA_EXCLUDEFROMCAPTURE value. A desktop client applies this to
     * one of its own windows with SetWindowDisplayAffinity().
     */
    public static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;

    private String type;
    private String sessionId;
    private String controlType;
    private Integer windowDisplayAffinity;
    private boolean captureExclusionRequested;
    private boolean captureExcluded;
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

    public Integer getWindowDisplayAffinity() {
        return windowDisplayAffinity;
    }

    public void setWindowDisplayAffinity(Integer windowDisplayAffinity) {
        this.windowDisplayAffinity = windowDisplayAffinity;
    }

    public boolean hasSupportedWindowDisplayAffinity() {
        return Integer.valueOf(WDA_EXCLUDEFROMCAPTURE).equals(windowDisplayAffinity);
    }

    /**
     * Explicit user-requested capture exclusion for this application's own
     * window. The desktop client must obtain consent before applying it.
     */
    public boolean isCaptureExclusionRequested() {
        return captureExclusionRequested;
    }

    public void setCaptureExclusionRequested(boolean captureExclusionRequested) {
        this.captureExclusionRequested = captureExclusionRequested;
    }

    public boolean isCaptureExcluded() {
        return captureExcluded;
    }

    public void setCaptureExcluded(boolean captureExcluded) {
        this.captureExcluded = captureExcluded;
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
