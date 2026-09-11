package com.remotesupport.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, Object> health() {
        return Map.of(
            "message", "Remote Support Server Running",
            "status", "OK",
            "port", 5000
        );
    }
}
