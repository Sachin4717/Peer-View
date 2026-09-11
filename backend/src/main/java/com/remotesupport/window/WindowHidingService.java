package com.remotesupport.window;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Applies capture exclusion and taskbar hiding to this application's own
 * Windows windows using JNA. Safe no-op on other operating systems.
 */
@Service
public class WindowHidingService {

    private static final Logger logger = LoggerFactory.getLogger(WindowHidingService.class);

    /**
     * Hides all windows of this process from screen capture (browser tab
     * sharing, Teams, OBS, ...) and optionally from the taskbar.
     *
     * @param hideFromTaskbar also remove the windows from the taskbar
     * @return result map with platform, hiddenWindowCount and captureExcluded
     */
    public Map<String, Object> hide(boolean hideFromTaskbar) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!WindowsWindowHelper.isWindows()) {
            result.put("platform", System.getProperty("os.name", "unknown"));
            result.put("captureExcluded", false);
            result.put("message", "Capture exclusion requires Windows.");
            return result;
        }

        int hidden = WindowsWindowHelper.hideAllCurrentProcessWindows(hideFromTaskbar);
        boolean excluded = hidden > 0;
        result.put("platform", System.getProperty("os.name"));
        result.put("hiddenWindowCount", hidden);
        result.put("captureExcluded", excluded);
        result.put("taskbarHidden", hideFromTaskbar && excluded);
        logger.info("Window hiding applied: {} window(s) excluded from capture", hidden);
        return result;
    }

    /** Restores capture visibility for all windows of this process. */
    public Map<String, Object> restore() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!WindowsWindowHelper.isWindows()) {
            result.put("platform", System.getProperty("os.name", "unknown"));
            result.put("captureRestored", false);
            return result;
        }
        int restored = WindowsWindowHelper.restoreAllCurrentProcessWindows();
        result.put("platform", System.getProperty("os.name"));
        result.put("restoredWindowCount", restored);
        logger.info("Window capture restored for {} window(s)", restored);
        return result;
    }
}
