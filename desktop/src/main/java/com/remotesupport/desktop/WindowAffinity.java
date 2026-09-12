package com.remotesupport.desktop;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies Windows SetWindowDisplayAffinity to every visible top-level window
 * of the desktop sender process so the sender's own support window never
 * appears in the shared screen capture, Teams, OBS or recordings.
 */
public final class WindowAffinity {

    private interface User32Ex extends StdCallLibrary {
        User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.UNICODE_OPTIONS);

        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, WinDef.DWORD dwAffinity);
    }

    private WindowAffinity() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    /** Excludes (exclude=true) or restores (false) every window of this process. */
    public static boolean setAllCurrentProcessWindows(boolean exclude) {
        if (!isWindows()) {
            return false;
        }
        int affinity = exclude ? 0x00000011 : 0x00000000; // WDA_EXCLUDEFROMCAPTURE / WDA_NONE
        int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        IntByReference ownerPid = new IntByReference();
        List<WinDef.HWND> windows = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            ownerPid.setValue(0);
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, ownerPid);
            if (ownerPid.getValue() == pid && User32.INSTANCE.IsWindowVisible(hWnd)) {
                windows.add(hWnd);
            }
            return true;
        }, null);
        boolean allOk = !windows.isEmpty();
        for (WinDef.HWND hWnd : windows) {
            boolean ok = User32Ex.INSTANCE.SetWindowDisplayAffinity(hWnd, new WinDef.DWORD(affinity));
            allOk &= ok;
        }
        return allOk;
    }
}
