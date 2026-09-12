package com.remotesupport.desktop;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * High-level application control service for AnyDesk-like remote control features.
 * Provides methods for window management, clipboard operations, and text input.
 */
public class ApplicationControlService {

    private interface User32Clipboard extends StdCallLibrary {
        User32Clipboard INSTANCE = Native.load("user32", User32Clipboard.class, W32APIOptions.UNICODE_OPTIONS);

        boolean OpenClipboard(WinDef.HWND hWndNewOwner);
        boolean CloseClipboard();
        boolean EmptyClipboard();
        Pointer GetClipboardData(int uFormat);
        Pointer SetClipboardData(int uFormat, Pointer hMem);
    }

    private static final int CF_TEXT = 1;
    private static final int CF_UNICODETEXT = 13;

    private ApplicationControlService() {
    }

    /**
     * Lists all visible application windows with their metadata.
     */
    public static List<ApplicationWindowManager.WindowInfo> listApplicationWindows() {
        return ApplicationWindowManager.getAllVisibleWindows();
    }

    /**
     * Finds an application window by title (partial match).
     */
    public static ApplicationWindowManager.WindowInfo findApplication(String titleOrPartial) {
        return ApplicationWindowManager.findWindowByTitle(titleOrPartial);
    }

    /**
     * Gets detailed information about a specific window.
     */
    public static ApplicationWindowManager.WindowInfo getApplicationInfo(long hwnd) {
        return ApplicationWindowManager.getWindowInfo(hwnd);
    }

    /**
     * Brings an application to focus (brings window to foreground).
     */
    public static boolean focusApplication(long hwnd) {
        return ApplicationWindowManager.focusWindow(hwnd);
    }

    /**
     * Maximizes an application window.
     */
    public static boolean maximizeApplication(long hwnd) {
        return ApplicationWindowManager.maximizeWindow(hwnd);
    }

    /**
     * Minimizes an application window.
     */
    public static boolean minimizeApplication(long hwnd) {
        return ApplicationWindowManager.minimizeWindow(hwnd);
    }

    /**
     * Closes an application window.
     */
    public static boolean closeApplication(long hwnd) {
        return ApplicationWindowManager.closeWindow(hwnd);
    }

    /**
     * Resizes and repositions a window.
     */
    public static boolean resizeApplication(long hwnd, int x, int y, int width, int height) {
        return ApplicationWindowManager.resizeWindow(hwnd, x, y, width, height);
    }

    /**
     * Sends text to the currently focused application via clipboard paste.
     * This is more reliable than character-by-character typing for large text blocks.
     */
    public static boolean sendTextViaClipboard(String text) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
            
            // Send Ctrl+V to paste
            InputInjector.keyDown('V', true);  // true = Ctrl pressed
            Thread.sleep(50);
            InputInjector.keyUp('V', true);
            Thread.sleep(50);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sends text character by character (slower, but works with any input field).
     */
    public static boolean sendTextCharByChar(String text) {
        try {
            for (char c : text.toCharArray()) {
                InputInjector.typeChar(c);
                Thread.sleep(20);  // Small delay between characters
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sends a key combination to the focused application.
     * @param key The key to press (e.g., 'A', '1', VK_F5)
     * @param shift Whether Shift is pressed
     * @param ctrl Whether Ctrl is pressed
     * @param alt Whether Alt is pressed
     */
    public static boolean sendKeyCombo(int key, boolean shift, boolean ctrl, boolean alt) {
        try {
            if (ctrl) InputInjector.keyDown(key, true);
            if (shift) InputInjector.keyDown(key, false);  // Shift
            if (alt) InputInjector.keyDown(key, false);    // Alt
            
            InputInjector.keyDown(key, false);
            Thread.sleep(50);
            InputInjector.keyUp(key, false);
            
            if (alt) InputInjector.keyUp(key, false);
            if (shift) InputInjector.keyUp(key, false);
            if (ctrl) InputInjector.keyUp(key, true);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Performs a double-click at normalized screen coordinates.
     */
    public static boolean doubleClick(double normalizedX, double normalizedY) {
        try {
            InputInjector.moveMouse(normalizedX, normalizedY);
            Thread.sleep(50);
            InputInjector.mouseButton(0, true);   // Left button down
            Thread.sleep(50);
            InputInjector.mouseButton(0, false);  // Left button up
            Thread.sleep(50);
            InputInjector.mouseButton(0, true);   // Left button down again
            Thread.sleep(50);
            InputInjector.mouseButton(0, false);  // Left button up
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Performs a right-click at normalized screen coordinates (shows context menu).
     */
    public static boolean rightClick(double normalizedX, double normalizedY) {
        try {
            InputInjector.moveMouse(normalizedX, normalizedY);
            Thread.sleep(50);
            InputInjector.mouseButton(2, true);   // Right button down
            Thread.sleep(50);
            InputInjector.mouseButton(2, false);  // Right button up
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Scrolls the mouse wheel at normalized coordinates.
     * @param normalizedX X coordinate (0..1)
     * @param normalizedY Y coordinate (0..1)
     * @param delta Scroll direction: positive = up, negative = down
     */
    public static boolean scroll(double normalizedX, double normalizedY, int delta) {
        try {
            InputInjector.moveMouse(normalizedX, normalizedY);
            Thread.sleep(50);
            InputInjector.mouseWheel(delta);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Drags from one point to another (click and drag).
     */
    public static boolean dragMouse(double fromX, double fromY, double toX, double toY, int durationMs) {
        try {
            InputInjector.moveMouse(fromX, fromY);
            Thread.sleep(100);
            InputInjector.mouseButton(0, true);  // Left button down
            
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < durationMs) {
                double progress = (double) (System.currentTimeMillis() - startTime) / durationMs;
                double x = fromX + (toX - fromX) * progress;
                double y = fromY + (toY - fromY) * progress;
                InputInjector.moveMouse(x, y);
                Thread.sleep(20);
            }
            
            InputInjector.moveMouse(toX, toY);
            InputInjector.mouseButton(0, false);  // Left button up
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Presses and holds a key (useful for games or key combinations).
     */
    public static boolean holdKey(int vkCode, long durationMs) {
        try {
            InputInjector.keyDown(vkCode, false);
            Thread.sleep(durationMs);
            InputInjector.keyUp(vkCode, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sends a sequence of key presses (e.g., Alt+Tab to switch windows).
     */
    public static boolean sendKeySequence(int[] keyCodes) {
        try {
            for (int keyCode : keyCodes) {
                InputInjector.keyDown(keyCode, false);
                Thread.sleep(50);
            }
            // Release in reverse order
            for (int i = keyCodes.length - 1; i >= 0; i--) {
                InputInjector.keyUp(keyCodes[i], false);
                Thread.sleep(50);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the list of running applications that match a search term.
     */
    public static List<ApplicationWindowManager.WindowInfo> searchApplications(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return listApplicationWindows();
        }
        return ApplicationWindowManager.findWindowsByClassName(searchTerm);
    }
}
