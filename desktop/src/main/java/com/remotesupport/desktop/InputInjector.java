package com.remotesupport.desktop;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Injects real OS-level mouse and keyboard input on Windows using the
 * SendInput() API. Events injected here behave exactly like physical user
 * input: they move the real cursor, click whatever window is under it and
 * type into the focused application.
 */
public final class InputInjector {

    /** Supplemental User32 mapping: SendInput is not exposed by JNA's User32. */
    private interface User32Input extends StdCallLibrary {
        User32Input INSTANCE = Native.load("user32", User32Input.class, W32APIOptions.DEFAULT_OPTIONS);

        int SendInput(int cInputs, WinUser.INPUT[] pInputs, int cbSize);
    }

    /**
     * WinUser definitions JNA's platform library lacks (MOUSEEVENTF_*,
     * KEYEVENTF_*).
     */
    private static final int MOUSEEVENTF_MOVE = 0x0001;
    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;
    private static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    private static final int MOUSEEVENTF_RIGHTUP = 0x0010;
    private static final int MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    private static final int MOUSEEVENTF_MIDDLEUP = 0x0040;
    private static final int MOUSEEVENTF_WHEEL = 0x0800;
    private static final int MOUSEEVENTF_VIRTUALDESK = 0x4000;
    private static final int MOUSEEVENTF_ABSOLUTE = 0x8000;

    private static final int KEYEVENTF_KEYUP = 0x0002;

    private static final int INPUT_MOUSE = 0;
    private static final int INPUT_KEYBOARD = 1;

    public static final int VK_LBUTTON = 0x01;
    public static final int VK_RBUTTON = 0x02;
    public static final int VK_MBUTTON = 0x04;

    // Virtual Key Codes for common keys
    public static final int VK_CONTROL = 0x11;
    public static final int VK_SHIFT = 0x10;
    public static final int VK_MENU = 0x12;  // Alt
    public static final int VK_F1 = 0x70;
    public static final int VK_F5 = 0x74;
    public static final int VK_ESCAPE = 0x1B;
    public static final int VK_RETURN = 0x0D;
    public static final int VK_TAB = 0x09;
    public static final int VK_DELETE = 0x46;
    public static final int VK_BACK = 0x08;

    private InputInjector() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    /** Moves the cursor to normalized (0..1) coordinates on the virtual screen. */
    public static void moveMouse(double nx, double ny) {
        if (!isWindows()) {
            return;
        }
        java.awt.Rectangle bounds = getVirtualScreenBounds();
        int x = bounds.x + (int) Math.round(nx * bounds.width);
        int y = bounds.y + (int) Math.round(ny * bounds.height);
        // Absolute 0..65535 normalized against the full virtual desktop.
        int absX = (x * 65535) / Math.max(1, bounds.width - 1);
        int absY = (y * 65535) / Math.max(1, bounds.height - 1);

        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(INPUT_MOUSE);
        input.input.setType(WinUser.MOUSEINPUT.class);
        input.input.mi.dx = new WinDef.LONG(absX);
        input.input.mi.dy = new WinDef.LONG(absY);
        input.input.mi.dwFlags = new WinDef.DWORD(MOUSEEVENTF_MOVE | MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_VIRTUALDESK);
        input.input.mi.mouseData = new WinDef.DWORD(0);
        input.input.mi.dwExtraInfo = new com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0);
        sendInput(input);
    }

    /** Presses or releases one mouse button (0=left, 1=middle, 2=right). */
    public static void mouseButton(int button, boolean down) {
        if (!isWindows()) {
            return;
        }
        int flags;
        if (button == 2) {
            flags = down ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP;
        } else if (button == 1) {
            flags = down ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_MIDDLEUP;
        } else {
            flags = down ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP;
        }
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(INPUT_MOUSE);
        input.input.setType(WinUser.MOUSEINPUT.class);
        input.input.mi.dwFlags = new WinDef.DWORD(flags);
        input.input.mi.mouseData = new WinDef.DWORD(0);
        input.input.mi.dwExtraInfo = new com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0);
        sendInput(input);
    }

    /** Scrolls the wheel by deltaNotches (positive = up). */
    public static void mouseWheel(int deltaNotches) {
        if (!isWindows()) {
            return;
        }
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(INPUT_MOUSE);
        input.input.setType(WinUser.MOUSEINPUT.class);
        input.input.mi.dwFlags = new WinDef.DWORD(MOUSEEVENTF_WHEEL);
        input.input.mi.mouseData = new WinDef.DWORD(deltaNotches * 120); // WHEEL_DELTA
        input.input.mi.dwExtraInfo = new com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0);
        sendInput(input);
    }

    /** Presses a virtual key (VK code). */
    public static void keyDown(int vk) {
        keyboard(vk, false);
    }

    /** Releases a virtual key (VK code). */
    public static void keyUp(int vk) {
        keyboard(vk, true);
    }

    /** Presses a key with optional Ctrl modifier. */
    public static void keyDown(int vk, boolean ctrl) {
        if (ctrl) {
            keyboard(VK_CONTROL, false);
        }
        keyboard(vk, false);
    }

    /** Releases a key with optional Ctrl modifier. */
    public static void keyUp(int vk, boolean ctrl) {
        keyboard(vk, true);
        if (ctrl) {
            keyboard(VK_CONTROL, true);
        }
    }

    /** Types a single character. */
    public static void typeChar(char c) {
        try {
            if (Character.isLetter(c)) {
                int vk = Character.toUpperCase(c);
                if (Character.isLowerCase(c)) {
                    keyDown(vk);
                    keyUp(vk);
                } else {
                    keyDown(VK_SHIFT);
                    keyDown(vk);
                    keyUp(vk);
                    keyUp(VK_SHIFT);
                }
            } else if (Character.isDigit(c)) {
                int vk = '0' + (c - '0');
                keyDown(vk);
                keyUp(vk);
            } else {
                // Special characters
                int vk = getVirtualKeyForChar(c);
                if (vk > 0) {
                    keyDown(vk);
                    keyUp(vk);
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    /** Types a string character by character. */
    public static void typeString(String text) {
        for (char c : text.toCharArray()) {
            typeChar(c);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Presses and releases a key quickly. */
    public static void tapKey(int vk) {
        keyDown(vk);
        keyUp(vk);
    }

    /** Maps common characters to virtual key codes. */
    private static int getVirtualKeyForChar(char c) {
        return switch (c) {
            case ' ' -> 0x20;      // Space
            case '!' -> 0x31;      // Shift+1
            case '@' -> 0x32;      // Shift+2
            case '#' -> 0x33;      // Shift+3
            case '$' -> 0x34;      // Shift+4
            case '%' -> 0x35;      // Shift+5
            case '^' -> 0x36;      // Shift+6
            case '&' -> 0x37;      // Shift+7
            case '*' -> 0x38;      // Shift+8
            case '(' -> 0x39;      // Shift+9
            case ')' -> 0x30;      // Shift+0
            case '-' -> 0xBD;      // Minus
            case '+' -> 0xBB;      // Plus
            case '=' -> 0xBB;      // Equals
            case '[' -> 0xDB;      // Left bracket
            case ']' -> 0xDD;      // Right bracket
            case '{' -> 0xDB;      // Shift+[
            case '}' -> 0xDD;      // Shift+]
            case ';' -> 0xBA;      // Semicolon
            case ':' -> 0xBA;      // Shift+;
            case '\'' -> 0xDE;     // Apostrophe
            case '"' -> 0xDE;      // Shift+'
            case ',' -> 0xBC;      // Comma
            case '<' -> 0xBC;      // Shift+,
            case '.' -> 0xBE;      // Period
            case '>' -> 0xBE;      // Shift+.
            case '/' -> 0xBF;      // Forward slash
            case '?' -> 0xBF;      // Shift+/
            case '\\' -> 0xDC;     // Backslash
            case '|' -> 0xDC;      // Shift+\
            case '`' -> 0xC0;      // Backtick
            case '~' -> 0xC0;      // Shift+`
            case '\n' -> 0x0D;     // Enter
            case '\t' -> 0x09;     // Tab
            default -> 0;
        };
    }

    private static void keyboard(int vk, boolean keyUp) {
        if (!isWindows()) {
            return;
        }
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(INPUT_KEYBOARD);
        input.input.setType(WinUser.KEYBDINPUT.class);
        input.input.ki.wVk = new WinDef.WORD(vk);
        input.input.ki.dwFlags = new WinDef.DWORD(keyUp ? KEYEVENTF_KEYUP : 0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0);
        sendInput(input);
    }

    private static void sendInput(WinUser.INPUT input) {
        try {
            User32Input.INSTANCE.SendInput(1, new WinUser.INPUT[] { input }, input.size());
        } catch (Throwable ignored) {
            // Input injection is a best-effort native call.
        }
    }

    private static java.awt.Rectangle getVirtualScreenBounds() {
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.awt.Rectangle virtual = new java.awt.Rectangle(0, 0, 0, 0);
        for (java.awt.GraphicsDevice device : ge.getScreenDevices()) {
            for (java.awt.GraphicsConfiguration config : device.getConfigurations()) {
                virtual = virtual.union(config.getBounds());
            }
        }
        return virtual;
    }
}
