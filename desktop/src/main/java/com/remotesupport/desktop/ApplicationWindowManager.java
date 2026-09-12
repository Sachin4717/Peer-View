package com.remotesupport.desktop;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages application windows on Windows: enumeration, focusing, and retrieving
 * window information. Used for AnyDesk-like application control features.
 */
public final class ApplicationWindowManager {

    private interface User32Extended extends StdCallLibrary {
        User32Extended INSTANCE = Native.load("user32", User32Extended.class, W32APIOptions.UNICODE_OPTIONS);

        boolean SetForegroundWindow(WinDef.HWND hWnd);
        boolean IsIconic(WinDef.HWND hWnd);
        boolean ShowWindow(WinDef.HWND hWnd, int nCmdShow);
        boolean SetWindowPos(WinDef.HWND hWnd, WinDef.HWND hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);
        int GetWindowTextLength(WinDef.HWND hWnd);
        int GetWindowText(WinDef.HWND hWnd, byte[] lpString, int nMaxCount);
        int GetClassName(WinDef.HWND hWnd, byte[] lpClassName, int nMaxCount);
        boolean GetWindowRect(WinDef.HWND hWnd, WinUser.RECT lpRect);
        boolean IsWindowVisible(WinDef.HWND hWnd);
        int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference lpdwProcessId);
    }

    private static final int SW_RESTORE = 9;
    private static final int SWP_NOZORDER = 0x0004;

    public static class WindowInfo {
        public long hwnd;  // Window handle as long
        public String title;
        public String className;
        public int x;
        public int y;
        public int width;
        public int height;
        public boolean visible;
        public int processId;

        public WindowInfo(long hwnd, String title, String className, int x, int y, int width, int height, 
                         boolean visible, int processId) {
            this.hwnd = hwnd;
            this.title = title;
            this.className = className;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.visible = visible;
            this.processId = processId;
        }

        @Override
        public String toString() {
            return String.format("Window(hwnd=%d, title='%s', class='%s', pos=(%d,%d), size=%dx%d, visible=%b, pid=%d)",
                    hwnd, title, className, x, y, width, height, visible, processId);
        }
    }

    private ApplicationWindowManager() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    /**
     * Enumerates all visible top-level windows.
     */
    public static List<WindowInfo> getAllVisibleWindows() {
        List<WindowInfo> windows = new ArrayList<>();
        if (!isWindows()) {
            return windows;
        }

        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            if (User32.INSTANCE.IsWindowVisible(hWnd)) {
                WindowInfo info = getWindowInfo(hWnd);
                if (info != null && !info.title.isEmpty()) {
                    windows.add(info);
                }
            }
            return true;
        }, null);

        return windows;
    }

    /**
     * Finds a window by partial title match (case-insensitive).
     */
    public static WindowInfo findWindowByTitle(String titlePart) {
        if (!isWindows()) {
            return null;
        }

        String searchTerm = titlePart.toLowerCase();
        List<WindowInfo> windows = getAllVisibleWindows();
        for (WindowInfo w : windows) {
            if (w.title.toLowerCase().contains(searchTerm)) {
                return w;
            }
        }
        return null;
    }

    /**
     * Finds windows by class name match (exact or partial, case-insensitive).
     */
    public static List<WindowInfo> findWindowsByClassName(String className) {
        List<WindowInfo> results = new ArrayList<>();
        if (!isWindows()) {
            return results;
        }

        String searchTerm = className.toLowerCase();
        List<WindowInfo> windows = getAllVisibleWindows();
        for (WindowInfo w : windows) {
            if (w.className.toLowerCase().contains(searchTerm)) {
                results.add(w);
            }
        }
        return results;
    }

    /**
     * Finds windows by process ID.
     */
    public static List<WindowInfo> findWindowsByProcessId(int processId) {
        List<WindowInfo> results = new ArrayList<>();
        if (!isWindows()) {
            return results;
        }

        List<WindowInfo> windows = getAllVisibleWindows();
        for (WindowInfo w : windows) {
            if (w.processId == processId) {
                results.add(w);
            }
        }
        return results;
    }

    /**
     * Brings a window to the foreground and maximizes if needed.
     */
    public static boolean focusWindow(long hWndValue) {
        if (!isWindows()) {
            return false;
        }

        WinDef.HWND hWnd = new WinDef.HWND(new com.sun.jna.Pointer(hWndValue));
        
        // If minimized, restore it
        if (User32Extended.INSTANCE.IsIconic(hWnd)) {
            User32Extended.INSTANCE.ShowWindow(hWnd, SW_RESTORE);
        }

        // Bring to foreground
        return User32Extended.INSTANCE.SetForegroundWindow(hWnd);
    }

    /**
     * Resizes and positions a window.
     */
    public static boolean resizeWindow(long hWndValue, int x, int y, int width, int height) {
        if (!isWindows()) {
            return false;
        }

        WinDef.HWND hWnd = new WinDef.HWND(new com.sun.jna.Pointer(hWndValue));
        return User32Extended.INSTANCE.SetWindowPos(hWnd, null, x, y, width, height, SWP_NOZORDER);
    }

    /**
     * Gets detailed information about a specific window.
     */
    public static WindowInfo getWindowInfo(long hWndValue) {
        if (!isWindows()) {
            return null;
        }

        WinDef.HWND hWnd = new WinDef.HWND(new com.sun.jna.Pointer(hWndValue));
        return getWindowInfo(hWnd);
    }

    /**
     * Retrieves window information from a window handle.
     */
    private static WindowInfo getWindowInfo(WinDef.HWND hWnd) {
        try {
            // Get window title
            int titleLength = User32Extended.INSTANCE.GetWindowTextLength(hWnd);
            String title = "";
            if (titleLength > 0) {
                byte[] titleBuffer = new byte[titleLength + 1];
                User32Extended.INSTANCE.GetWindowText(hWnd, titleBuffer, titleLength + 1);
                title = Native.toString(titleBuffer);
            }

            // Get window class name
            byte[] classBuffer = new byte[256];
            User32Extended.INSTANCE.GetClassName(hWnd, classBuffer, 256);
            String className = Native.toString(classBuffer);

            // Get window bounds
            WinUser.RECT rect = new WinUser.RECT();
            User32Extended.INSTANCE.GetWindowRect(hWnd, rect);
            int width = rect.right - rect.left;
            int height = rect.bottom - rect.top;

            // Get process ID
            IntByReference processId = new IntByReference();
            User32Extended.INSTANCE.GetWindowThreadProcessId(hWnd, processId);

            // Check if visible
            boolean visible = User32Extended.INSTANCE.IsWindowVisible(hWnd);

            // Convert HWND pointer to long
            long hwndValue = com.sun.jna.Pointer.nativeValue(hWnd.getPointer());

            return new WindowInfo(hwndValue, title, className, rect.left, rect.top, width, height, visible, processId.getValue());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Closes a window.
     */
    public static boolean closeWindow(long hWndValue) {
        if (!isWindows()) {
            return false;
        }

        WinDef.HWND hWnd = new WinDef.HWND(new com.sun.jna.Pointer(hWndValue));
        User32.INSTANCE.PostMessage(hWnd, WinUser.WM_CLOSE, null, null);
        return true;
    }

    /**
     * Maximizes a window.
     */
    public static boolean maximizeWindow(long hWndValue) {
        if (!isWindows()) {
            return false;
        }

        WinDef.HWND hWnd = new WinDef.HWND(new com.sun.jna.Pointer(hWndValue));
        User32Extended.INSTANCE.ShowWindow(hWnd, WinUser.SW_MAXIMIZE);
        return true;
    }

    /**
     * Minimizes a window.
     */
    public static boolean minimizeWindow(long hWndValue) {
        if (!isWindows()) {
            return false;
        }

        WinDef.HWND hWnd = new WinDef.HWND(new com.sun.jna.Pointer(hWndValue));
        User32Extended.INSTANCE.ShowWindow(hWnd, WinUser.SW_MINIMIZE);
        return true;
    }
}
