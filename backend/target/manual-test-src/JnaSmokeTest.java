import com.remotesupport.window.WindowsWindowHelper;
import com.sun.jna.platform.win32.WinDef;
import java.util.List;

public class JnaSmokeTest {
    public static void main(String[] args) {
        System.out.println("os.name=" + System.getProperty("os.name"));
        System.out.println("isWindows=" + WindowsWindowHelper.isWindows());
        List<WinDef.HWND> windows = WindowsWindowHelper.findCurrentProcessWindows();
        System.out.println("visibleWindowsInProcess=" + windows.size());
        boolean excluded = WindowsWindowHelper.excludeFromCapture(windows.get(0));
        System.out.println("excludeFromCapture=" + excluded);
        boolean taskbar = WindowsWindowHelper.hideFromTaskbar(windows.get(0));
        System.out.println("hideFromTaskbar=" + taskbar);
        int restored = WindowsWindowHelper.restoreAllCurrentProcessWindows();
        System.out.println("restored=" + restored);
        System.out.println("SMOKE OK");
    }
}
