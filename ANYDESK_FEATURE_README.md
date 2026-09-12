# AnyDesk-like Remote Application Control Feature

## Overview

This document describes the new remote application control features added to Peer-View, similar to AnyDesk. These features allow remote control of any Windows application including window management, text input, keyboard control, and mouse interactions.

## Architecture

### Desktop Components

#### 1. **ApplicationWindowManager.java**
Manages Windows application windows with the following capabilities:
- **Window Enumeration**: Lists all visible top-level windows
- **Window Search**: Find windows by title, class name, or process ID
- **Window Control**: Focus, maximize, minimize, resize, and close windows
- **Window Information**: Retrieve detailed window metadata (position, size, visibility, etc.)

**Key Methods**:
```java
List<WindowInfo> getAllVisibleWindows()
WindowInfo findWindowByTitle(String titlePart)
List<WindowInfo> findWindowsByClassName(String className)
boolean focusWindow(long hwnd)
boolean maximizeWindow(long hwnd)
boolean minimizeWindow(long hwnd)
boolean closeWindow(long hwnd)
boolean resizeWindow(long hwnd, int x, int y, int width, int height)
```

#### 2. **ApplicationControlService.java**
High-level service providing AnyDesk-like application control operations:
- **Application Listing & Search**: Find running applications
- **Window Management**: Focus, close, minimize, maximize applications
- **Text Input**: Send text via clipboard or character-by-character
- **Keyboard Input**: Send key presses, key combinations, sequences
- **Mouse Control**: Click, drag, scroll, right-click
- **Advanced Interactions**: Double-click, context menus

**Key Methods**:
```java
List<WindowInfo> listApplicationWindows()
ApplicationWindowInfo findApplication(String titleOrPartial)
boolean focusApplication(long hwnd)
boolean sendTextViaClipboard(String text)
boolean sendTextCharByChar(String text)
boolean doubleClick(double normalizedX, double normalizedY)
boolean rightClick(double normalizedX, double normalizedY)
boolean scroll(double normalizedX, double normalizedY, int delta)
boolean dragMouse(double fromX, double fromY, double toX, double toY, int durationMs)
```

#### 3. **InputInjector.java Enhancements**
Extended with new keyboard and text input methods:
- **Keyboard Control**: 
  - `keyDown(int vk)`, `keyUp(int vk)` - Basic key control
  - `keyDown(int vk, boolean ctrl)` - Key press with Ctrl modifier
  - `typeChar(char c)` - Type a single character
  - `typeString(String text)` - Type a full string
  - `tapKey(int vk)` - Quick key press/release

- **Virtual Key Constants**:
  - `VK_CONTROL`, `VK_SHIFT`, `VK_MENU` (Alt)
  - `VK_F1`, `VK_F5`, etc. (Function keys)
  - `VK_RETURN`, `VK_TAB`, `VK_ESCAPE`, etc.

### Backend Components

#### 1. **ApplicationController.java**
REST API endpoints for application control:

**Endpoints**:
- `GET /api/control/windows/list` - List all visible windows
- `POST /api/control/windows/find` - Find window by search term
- `POST /api/control/execute` - Execute control command
- `GET /api/control/windows/{hwnd}` - Get window info by handle
- `GET /api/control/health` - Health check

#### 2. **DTOs**
- `WindowInfoDTO` - Window metadata and state
- `ApplicationControlRequest` - Control command specification

### Frontend Components

#### **ApplicationControl.jsx & ApplicationControl.css**
React component providing UI for:
- **Windows Tab**: List, search, and control windows
- **Text Input Tab**: Send text to remote application
- **Keyboard Tab**: Send key presses and shortcuts
- **Mouse Control Tab**: Interactive mouse control with click, drag, and scroll

## Usage Examples

### Java Backend Usage

#### List all visible windows
```java
List<ApplicationWindowManager.WindowInfo> windows = ApplicationWindowManager.getAllVisibleWindows();
for (WindowInfo w : windows) {
    System.out.println(w.title + " - " + w.className);
}
```

#### Focus a specific window
```java
WindowInfo notepad = ApplicationWindowManager.findWindowByTitle("Notepad");
if (notepad != null) {
    ApplicationWindowManager.focusWindow(notepad.hwnd);
}
```

#### Send text to focused application
```java
ApplicationControlService.sendTextViaClipboard("Hello, World!");
// or character by character
ApplicationControlService.sendTextCharByChar("Hello");
```

#### Perform mouse actions
```java
// Click at screen coordinates
ApplicationControlService.doubleClick(0.5, 0.5);  // Normalized (0..1)

// Scroll
ApplicationControlService.scroll(0.5, 0.5, 5);  // Delta: positive = up, negative = down

// Drag
ApplicationControlService.dragMouse(0.2, 0.2, 0.8, 0.8, 500);  // 500ms drag
```

#### Send keyboard input
```java
// Send Ctrl+A
ApplicationControlService.sendKeyCombo(0x41, false, true, false);

// Send key sequence
ApplicationControlService.sendKeySequence(new int[]{0x12, 0x09});  // Alt+Tab

// Hold key for duration
ApplicationControlService.holdKey(InputInjector.VK_CONTROL, 1000);  // 1 second
```

### WebSocket Protocol

Commands are sent via WebSocket as JSON:

```json
{
  "type": "CONTROL_APP",
  "data": {
    "action": "focus",
    "hwnd": 12345678
  }
}
```

**Supported Actions**:
- `focus` - Bring window to foreground (requires: `hwnd`)
- `minimize` - Minimize window (requires: `hwnd`)
- `maximize` - Maximize window (requires: `hwnd`)
- `close` - Close window (requires: `hwnd`)
- `resize` - Resize and move window (requires: `hwnd`, `x`, `y`, `width`, `height`)
- `click` - Click at position (requires: `normalizedX`, `normalizedY`)
- `rightclick` - Right-click at position (requires: `normalizedX`, `normalizedY`)
- `doubleclick` - Double-click at position (requires: `normalizedX`, `normalizedY`)
- `scroll` - Scroll at position (requires: `normalizedX`, `normalizedY`, `delta`)
- `drag` - Drag mouse (requires: `fromX`, `fromY`, `toX`, `toY`, `duration`)
- `text` - Send text (requires: `text`)
- `key` - Send key press (requires: `keyCode`, optional: `shift`, `ctrl`, `alt`)

### Frontend Usage

#### Get list of windows
```javascript
socket.send(JSON.stringify({
  type: 'GET_WINDOWS',
  data: { search: 'notepad' }
}));
```

#### Focus a window
```javascript
socket.send(JSON.stringify({
  type: 'CONTROL_APP',
  data: {
    action: 'focus',
    hwnd: 12345678
  }
}));
```

#### Send text
```javascript
socket.send(JSON.stringify({
  type: 'CONTROL_APP',
  data: {
    action: 'text',
    text: 'Hello, Remote World!'
  }
}));
```

#### Click at position
```javascript
socket.send(JSON.stringify({
  type: 'CONTROL_APP',
  data: {
    action: 'click',
    normalizedX: 0.5,  // Center X
    normalizedY: 0.5   // Center Y
  }
}));
```

## Integration Steps

### 1. Compile Desktop Application
```bash
cd desktop
mvn clean package
```

### 2. Compile Backend
```bash
cd backend
mvn clean package
```

### 3. Run Backend Server
```bash
mvn -f backend spring-boot:run
```

### 4. Run Desktop Sender
```bash
java -jar desktop/target/desktop-sender-1.0.0-shaded.jar
```

### 5. Use Frontend Application
Navigate to the frontend and import `ApplicationControl` component in your page:
```jsx
import ApplicationControl from './pages/ApplicationControl';

// In your component:
<ApplicationControl socketRef={socketRef} />
```

## Security Considerations

⚠️ **Important**: This is a powerful feature that allows remote control. Always:

1. **Use over secure connections** - Use WebSocket over TLS (wss://)
2. **Implement authentication** - Verify remote user identity
3. **Implement authorization** - Only allow trusted users to control applications
4. **Log all actions** - Track which user performed which control actions
5. **Limit scope** - Consider restricting which applications can be controlled
6. **User confirmation** - Show notifications to the local user when remote control occurs

## Limitations & Notes

- **Windows Only**: Currently supports Windows 10+ (uses Win32 API via JNA)
- **Process Privileges**: Some operations require appropriate process privileges
- **Special Characters**: Text input handles common ASCII and extended characters
- **Performance**: Text via clipboard is faster for large text blocks
- **Normalized Coordinates**: Mouse positions use 0..1 scale across full virtual desktop

## Advanced Features

### Custom Application Detection
```java
// Find all Chrome windows
List<ApplicationWindowManager.WindowInfo> chromeWindows = 
    ApplicationWindowManager.findWindowsByClassName("Chrome");

// Find specific Excel file
ApplicationWindowManager.WindowInfo excelSheet = 
    ApplicationWindowManager.findWindowByTitle("Budget.xlsx");
```

### Window Monitoring
Implement periodic window listing for dynamic UI updates:
```java
Timer timer = new Timer();
timer.scheduleAtFixedRate(() -> {
    List<ApplicationWindowManager.WindowInfo> windows = 
        ApplicationWindowManager.getAllVisibleWindows();
    // Update UI with current windows
}, 0, 1000);  // Update every second
```

### Clipboard Integration
For secure text transfer:
```java
String largeText = "... very long text ...";
ApplicationControlService.sendTextViaClipboard(largeText);
```

## Troubleshooting

### Windows Not Listed
- Check that the desktop sender is running with proper permissions
- Verify WebSocket connection is active
- Try searching for specific window titles instead of listing all

### Input Not Working
- Ensure the target application window is in focus
- For system applications, may require elevated privileges
- Try character-by-character input instead of clipboard for problematic apps

### Performance Issues
- Reduce window list refresh frequency
- Use clipboard for bulk text transfers
- Optimize mouse movement intervals

## Future Enhancements

- [ ] macOS/Linux support
- [ ] OCR for on-screen text recognition
- [ ] Window screenshot/thumbnail preview
- [ ] Macro recording and playback
- [ ] Gesture recognition for trackpad inputs
- [ ] Application-specific context awareness
- [ ] Multi-monitor support improvements
- [ ] Touch input support
