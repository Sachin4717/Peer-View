package com.remotesupport.controller;

import com.remotesupport.service.SessionService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final SessionService sessionService;

    public HealthController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/")
    public Map<String, Object> health() {
        return Map.of(
            "message", "Remote Support Server Running",
            "status", "OK",
            "port", 5001
        );
    }

    /** Debug helper: the currently active session ids. */
    @GetMapping("/api/sessions")
    public Map<String, Object> sessions() {
        return Map.of("activeSessions", sessionService.activeSessionIds());
    }
}
