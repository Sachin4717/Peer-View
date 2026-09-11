package com.remotesupport.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SessionMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesCaptureExclusionRequestUsingTheWindowsAffinityValue() throws Exception {
        SessionMessage message = new SessionMessage();
        message.setType("display-affinity");
        message.setCaptureExclusionRequested(true);
        message.setWindowDisplayAffinity(SessionMessage.WDA_EXCLUDEFROMCAPTURE);

        String json = objectMapper.writeValueAsString(message);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertThat(jsonNode.get("type").asText()).isEqualTo("display-affinity");
        assertThat(jsonNode.get("captureExclusionRequested").asBoolean()).isTrue();
        assertThat(jsonNode.get("windowDisplayAffinity").asInt())
                .isEqualTo(SessionMessage.WDA_EXCLUDEFROMCAPTURE);
    }

    @Test
    void deserializesTheCaptureExclusionResultAndIgnoresUnknownFields() throws Exception {
        String json = """
                {
                  "type": "display-affinity-result",
                  "windowDisplayAffinity": 17,
                  "captureExcluded": true,
                  "futureField": "ignored"
                }
                """;

        SessionMessage message = objectMapper.readValue(json, SessionMessage.class);

        assertThat(message.getType()).isEqualTo("display-affinity-result");
        assertThat(message.getWindowDisplayAffinity())
                .isEqualTo(SessionMessage.WDA_EXCLUDEFROMCAPTURE);
        assertThat(message.isCaptureExcluded()).isTrue();
    }

    @Test
    void onlyRecognizesTheSupportedCaptureExclusionAffinity() {
        SessionMessage supported = new SessionMessage();
        supported.setWindowDisplayAffinity(SessionMessage.WDA_EXCLUDEFROMCAPTURE);
        SessionMessage unsupported = new SessionMessage();
        unsupported.setWindowDisplayAffinity(0);

        assertThat(supported.hasSupportedWindowDisplayAffinity()).isTrue();
        assertThat(unsupported.hasSupportedWindowDisplayAffinity()).isFalse();
    }

    @Test
    void serializesControlRequestWithControlType() throws Exception {
        SessionMessage message = new SessionMessage();
        message.setType("control-request");
        message.setSessionId("12345678");
        message.setControlType("mouse");

        String json = objectMapper.writeValueAsString(message);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertThat(jsonNode.get("type").asText()).isEqualTo("control-request");
        assertThat(jsonNode.get("sessionId").asText()).isEqualTo("12345678");
        assertThat(jsonNode.get("controlType").asText()).isEqualTo("mouse");
    }

    @Test
    void deserializesControlResponseAndPreservesTheAllowedFlag() throws Exception {
        String json = """
                {
                  "type": "control-response",
                  "sessionId": "12345678",
                  "controlType": "keyboard",
                  "allowed": true
                }
                """;

        SessionMessage message = objectMapper.readValue(json, SessionMessage.class);

        assertThat(message.getType()).isEqualTo("control-response");
        assertThat(message.getSessionId()).isEqualTo("12345678");
        assertThat(message.getControlType()).isEqualTo("keyboard");
        assertThat(message.isAllowed()).isTrue();
    }

    @Test
    void deserializesControlEventPayload() throws Exception {
        String json = """
                {
                  "type": "control-event",
                  "sessionId": "12345678",
                  "event": {
                    "kind": "mousemove",
                    "x": 0.5,
                    "y": 0.25
                  }
                }
                """;

        SessionMessage message = objectMapper.readValue(json, SessionMessage.class);

        assertThat(message.getType()).isEqualTo("control-event");
        assertThat(message.getEvent().get("kind").asText()).isEqualTo("mousemove");
        assertThat(message.getEvent().get("x").asDouble()).isEqualTo(0.5);
        assertThat(message.getEvent().get("y").asDouble()).isEqualTo(0.25);
    }

    @Test
    void controlEventIsMissingWhenNotProvided() {
        SessionMessage message = new SessionMessage();

        assertThat(message.getEvent()).isNull();
    }
}
