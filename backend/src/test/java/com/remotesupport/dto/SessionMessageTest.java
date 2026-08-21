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
}
