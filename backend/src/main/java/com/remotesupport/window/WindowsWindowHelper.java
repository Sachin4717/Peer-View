package com.remotesupport.window;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.List;

/**
 * JNA-based Windows helper used by this application's own window host.
 *
 * Two effects are provided:
 * <ul>
 * <li>Hide from screen capture (Chrome tab sharing, Teams, OBS, ...) by
 * applying {@code SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)} to
 * every top-level window of this process.</li>
 * <li>Hide from the taskbar and Alt-Tab by adding the
 * {@code WS_EX_TOOLWINDOW} extended window style.</li>
 * </ul>
 *
 * All calls fail gracefully on non-Windows platforms.
 */
public final class WindowsWindowHelper {

    /**
     * User32 functions that JNA's User32 interface does not map yet.
     * W32APIOptions selects the correct Unicode/64-bit binding.
     */
    private interface User32Ex extends StdCallLibrary {
        User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.UNICODE_OPTIONS);

        boolean SetWindowDisplayAffinity(HWND hWnd, DWORD dwAffinity);

        BaseTSD.LONG_PTR GetWindowLongPtr(HWND hWnd, int nIndex);

        BaseTSD.LONG_PTR SetWindowLongPtr(HWND hWnd, int nIndex, BaseTSD.LONG_PTR dwNewLong);
    }

    /** Windows WDA_EXCLUDEFROMCAPTURE. The window is excluded from capture. */
    public static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;

    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;

    private WindowsWindowHelper() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    /**
     * Enumerates the visible top-level windows that belong to the current
     * process.
     */
    public static List<WinDef.HWND> findCurrentProcessWindows() {
        List<WinDef.HWND> windows = new ArrayList<>();
        if (!isWindows()) {
            return windows;
        }
        int processId = Kernel32.INSTANCE.GetCurrentProcessId();
        IntByReference ownerPid = new IntByReference();
        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            ownerPid.setValue(0);
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, ownerPid);
            if (ownerPid.getValue() == processId && User32.INSTANCE.IsWindowVisible(hWnd)) {
                windows.add(hWnd);
            }
            return true;
        }, null);
        return windows;
    }

    /** Excludes one window from screen capture. Returns true on success. */
    public static boolean excludeFromCapture(WinDef.HWND hWnd) {
        if (!isWindows() || hWnd == null) {
            return false;
        }
        return setWindowDisplayAffinity(hWnd, WDA_EXCLUDEFROMCAPTURE);
    }

    /** Adds WS_EX_TOOLWINDOW so the window leaves the taskbar and Alt-Tab. */
    public static boolean hideFromTaskbar(WinDef.HWND hWnd) {
        if (!isWindows() || hWnd == null) {
            return false;
        }
        BaseTSD.LONG_PTR style = User32Ex.INSTANCE.GetWindowLongPtr(hWnd, GWL_EXSTYLE);
        User32Ex.INSTANCE.SetWindowLongPtr(hWnd, GWL_EXSTYLE,
                new BaseTSD.LONG_PTR(style.longValue() | WS_EX_TOOLWINDOW));
        return true;
    }

    /**
     * Applies both effects to all windows of the current process.
     *
     * @param hideFromTaskbar also remove the windows from the taskbar
     * @return the number of windows successfully excluded from capture
     */
    public static int hideAllCurrentProcessWindows(boolean hideFromTaskbar) {
        int hidden = 0;
        for (WinDef.HWND hWnd : findCurrentProcessWindows()) {
            if (excludeFromCapture(hWnd)) {
                hidden++;
            }
            if (hideFromTaskbar) {
                hideFromTaskbar(hWnd);
            }
        }
        return hidden;
    }

    /** Restores capture for all windows of the current process. */
    public static int restoreAllCurrentProcessWindows() {
        int restored = 0;
        for (WinDef.HWND hWnd : findCurrentProcessWindows()) {
            // Affinity 0 (WDA_NONE) removes the capture exclusion.
            if (setWindowDisplayAffinity(hWnd, 0)) {
                restored++;
            }
        }
        return restored;
    }

    /**
     * SetWindowDisplayAffinity is not mapped by JNA's User32, so it is
     * resolved through the supplementary User32Ex mapping.
     */
    private static boolean setWindowDisplayAffinity(WinDef.HWND hWnd, int affinity) {
        try {
            return User32Ex.INSTANCE.SetWindowDisplayAffinity(
                    (HWND) hWnd, new DWORD(affinity));
        } catch (UnsatisfiedLinkError | RuntimeException error) {
            return false;
        }
    }
}
