package com.remotesupport.controller;

import com.remotesupport.dto.ApplicationControlRequest;
import com.remotesupport.dto.WindowInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for application control commands (like AnyDesk).
 * These commands are forwarded to the desktop sender via WebSocket.
 */
@RestController
@RequestMapping("/api/control")
@CrossOrigin(origins = "*")
public class ApplicationController {

    /**
     * Lists all visible application windows.
     */
    @GetMapping("/windows/list")
    public ResponseEntity<Map<String, Object>> listWindows() {
        Map<String, Object> response = new HashMap<>();
        try {
            // In a real implementation, this would communicate with the desktop sender
            // via WebSocket to get the actual window list
            List<WindowInfoDTO> windows = new ArrayList<>();
            response.put("status", "success");
            response.put("windows", windows);
            response.put("count", windows.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Finds an application by title or class name.
     */
    @PostMapping("/windows/find")
    public ResponseEntity<Map<String, Object>> findWindow(@RequestBody ApplicationControlRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String searchTerm = request.getSearchTerm();
            if (searchTerm == null || searchTerm.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Search term is required");
                return ResponseEntity.badRequest().body(response);
            }

            // In a real implementation, this would communicate with the desktop sender
            response.put("status", "success");
            response.put("message", "Finding window: " + searchTerm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Sends an application control command.
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeControl(@RequestBody ApplicationControlRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String action = request.getAction();
            if (action == null || action.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Action is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate based on action type
            switch (action.toLowerCase()) {
                case "focus":
                case "minimize":
                case "maximize":
                case "close":
                    if (request.getHwnd() == null) {
                        response.put("status", "error");
                        response.put("message", "Window handle (hwnd) is required for " + action);
                        return ResponseEntity.badRequest().body(response);
                    }
                    break;

                case "click":
                case "rightclick":
                    if (request.getNormalizedX() == null || request.getNormalizedY() == null) {
                        response.put("status", "error");
                        response.put("message", "normalizedX and normalizedY are required for click");
                        return ResponseEntity.badRequest().body(response);
                    }
                    break;

                case "scroll":
                    if (request.getNormalizedX() == null || request.getNormalizedY() == null || request.getDelta() == null) {
                        response.put("status", "error");
                        response.put("message", "normalizedX, normalizedY, and delta are required for scroll");
                        return ResponseEntity.badRequest().body(response);
                    }
                    break;

                case "text":
                    if (request.getText() == null || request.getText().isEmpty()) {
                        response.put("status", "error");
                        response.put("message", "Text is required for text action");
                        return ResponseEntity.badRequest().body(response);
                    }
                    break;

                case "key":
                    if (request.getKeyCode() == null) {
                        response.put("status", "error");
                        response.put("message", "Key code is required for key action");
                        return ResponseEntity.badRequest().body(response);
                    }
                    break;

                case "resize":
                    if (request.getHwnd() == null || request.getX() == null || request.getY() == null
                            || request.getWidth() == null || request.getHeight() == null) {
                        response.put("status", "error");
                        response.put("message", "hwnd, x, y, width, and height are required for resize");
                        return ResponseEntity.badRequest().body(response);
                    }
                    break;

                default:
                    response.put("status", "error");
                    response.put("message", "Unknown action: " + action);
                    return ResponseEntity.badRequest().body(response);
            }

            // In a real implementation, send the command to the desktop sender via WebSocket
            response.put("status", "pending");
            response.put("action", action);
            response.put("message", "Command queued for execution");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Gets detailed info about a specific window.
     */
    @GetMapping("/windows/{hwnd}")
    public ResponseEntity<Map<String, Object>> getWindowInfo(@PathVariable long hwnd) {
        Map<String, Object> response = new HashMap<>();
        try {
            // In a real implementation, this would get window info from the desktop sender
            response.put("status", "success");
            response.put("hwnd", hwnd);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Health check for the control API.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "ApplicationController");
        return ResponseEntity.ok(response);
    }
}
