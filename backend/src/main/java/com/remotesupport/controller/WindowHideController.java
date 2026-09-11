package com.remotesupport.controller;

import com.remotesupport.window.WindowHidingService;
import com.remotesupport.window.WindowsWindowHelper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP access to the JNA window hiding feature. */
@RestController
@RequestMapping("/api/window")
public class WindowHideController {

    private final WindowHidingService windowHidingService;

    public WindowHideController(WindowHidingService windowHidingService) {
        this.windowHidingService = windowHidingService;
    }

    /**
     * Hides this application's windows from screen capture and optionally from
     * the taskbar. Example: POST /api/window/hide?hideFromTaskbar=true
     */
    @PostMapping("/hide")
    public Map<String, Object> hide(
            @RequestParam(defaultValue = "false") boolean hideFromTaskbar) {
        return windowHidingService.hide(hideFromTaskbar);
    }

    /** Restores capture visibility for all windows of this application. */
    @PostMapping("/restore")
    public Map<String, Object> restore() {
        return windowHidingService.restore();
    }

    /** Convenience GET so the state can be checked from a browser. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "platform", System.getProperty("os.name", "unknown"),
                "supported", WindowsWindowHelper.isWindows());
    }
}
