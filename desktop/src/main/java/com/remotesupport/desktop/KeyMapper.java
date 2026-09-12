package com.remotesupport.desktop;

import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * Maps browser KeyboardEvent key/code values (e.g. "a", "Enter", "ArrowLeft")
 * to Windows virtual-key codes. The VK constants of java.awt.event.KeyEvent
 * match the Win32 VK_ values for the keys involved.
 */
public final class KeyMapper {

    private KeyMapper() {
    }

    private static final java.util.regex.Pattern CODE_PATTERN =
            java.util.regex.Pattern.compile("(?:Key|Digit)([A-Z0-9])");

    private static final Map<String, Integer> NUMPAD_OPS = Map.of(
            "NumpadAdd", KeyEvent.VK_ADD,
            "NumpadSubtract", KeyEvent.VK_SUBTRACT,
            "NumpadMultiply", KeyEvent.VK_MULTIPLY,
            "NumpadDivide", KeyEvent.VK_DIVIDE,
            "NumpadEnter", KeyEvent.VK_ENTER,
            "NumpadDecimal", KeyEvent.VK_DECIMAL);

    private static final Map<String, Integer> SPECIAL = Map.ofEntries(
            Map.entry("Enter", KeyEvent.VK_ENTER),
            Map.entry("Backspace", KeyEvent.VK_BACK_SPACE),
            Map.entry("Delete", KeyEvent.VK_DELETE),
            Map.entry("Tab", KeyEvent.VK_TAB),
            Map.entry("Escape", KeyEvent.VK_ESCAPE),
            Map.entry("ArrowUp", KeyEvent.VK_UP),
            Map.entry("ArrowDown", KeyEvent.VK_DOWN),
            Map.entry("ArrowLeft", KeyEvent.VK_LEFT),
            Map.entry("ArrowRight", KeyEvent.VK_RIGHT),
            Map.entry("Home", KeyEvent.VK_HOME),
            Map.entry("End", KeyEvent.VK_END),
            Map.entry("PageUp", KeyEvent.VK_PAGE_UP),
            Map.entry("PageDown", KeyEvent.VK_PAGE_DOWN),
            Map.entry("Insert", KeyEvent.VK_INSERT),
            Map.entry("Space", KeyEvent.VK_SPACE),
            Map.entry("Shift", KeyEvent.VK_SHIFT),
            Map.entry("Control", KeyEvent.VK_CONTROL),
            Map.entry("Alt", KeyEvent.VK_ALT),
            Map.entry("Meta", KeyEvent.VK_META),
            Map.entry("CapsLock", KeyEvent.VK_CAPS_LOCK),
            Map.entry("NumLock", KeyEvent.VK_NUM_LOCK),
            Map.entry("ScrollLock", KeyEvent.VK_SCROLL_LOCK),
            Map.entry("Pause", KeyEvent.VK_PAUSE),
            Map.entry("PrintScreen", KeyEvent.VK_PRINTSCREEN),
            Map.entry("F1", KeyEvent.VK_F1), Map.entry("F2", KeyEvent.VK_F2),
            Map.entry("F3", KeyEvent.VK_F3), Map.entry("F4", KeyEvent.VK_F4),
            Map.entry("F5", KeyEvent.VK_F5), Map.entry("F6", KeyEvent.VK_F6),
            Map.entry("F7", KeyEvent.VK_F7), Map.entry("F8", KeyEvent.VK_F8),
            Map.entry("F9", KeyEvent.VK_F9), Map.entry("F10", KeyEvent.VK_F10),
            Map.entry("F11", KeyEvent.VK_F11), Map.entry("F12", KeyEvent.VK_F12),
            Map.entry("Dead", 0));

    /** Resolves a browser key event description to a Windows VK code, or 0. */
    public static int toVirtualKey(String key, String code) {
        if (key == null) {
            return 0;
        }
        Integer special = SPECIAL.get(key);
        if (special != null) {
            return special;
        }
        if (key.length() == 1) {
            char ch = key.charAt(0);
            if (ch == ' ') {
                return KeyEvent.VK_SPACE;
            }
            // Prefer the physical-code letter/digit so shortcuts work in any
            // layout: browsers report code as "KeyA" / "Digit5".
            if (code != null) {
                var matcher = CODE_PATTERN.matcher(code);
                if (matcher.matches()) {
                    return KeyEvent.getExtendedKeyCodeForChar(
                            matcher.group(1).charAt(0));
                }
            }
            return KeyEvent.getExtendedKeyCodeForChar(ch);
        }
        // Numpad digits and operators via the key name.
        if (key.startsWith("Numpad") && key.length() > 6) {
            char ch = key.charAt(6);
            if (Character.isDigit(ch)) {
                return KeyEvent.VK_NUMPAD0 + (ch - '0');
            }
            Integer op = NUMPAD_OPS.get(key);
            if (op != null) {
                return op;
            }
        }
        return 0;
    }
}
