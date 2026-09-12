# Application Control Feature - Integration Guide

## ✅ What Was Fixed

### 1. **Backend WebSocket Handler**
- Added handlers for `get-windows` message type to relay window list requests
- Added handlers for `control-app` message type to relay control commands
- Extended `SessionMessage` DTO with `data` field for flexible payload handling
- Commands are properly relayed between session participants (receiver → sender)

### 2. **Frontend Component**
- Fixed message structure to match WebSocket protocol (`type`, `sessionId`, `data`)
- Updated keyboard tab with proper virtual key codes (VK_* constants)
- Enhanced keyboard shortcuts (Ctrl+A, Ctrl+C, Ctrl+V, etc.)
- Fixed mouse control tab with canvas and interactive buttons
- Added proper status feedback with animations
- Added scroll wheel support for the mouse canvas
- Normalized mouse coordinates (0..1) for multi-monitor support

### 3. **Keyboard Tab Features**
✅ Function Keys: F1, F5, F12
✅ Navigation: Tab, Enter, Backspace, Escape
✅ Shortcuts: Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+Z, Ctrl+Y, Ctrl+X, Ctrl+S, Ctrl+F
✅ Arrow Keys: Up, Down, Left, Right

### 4. **Mouse Control Tab Features**
✅ Interactive Canvas: Click, drag, wheel scroll
✅ Mouse Buttons: Left Click, Right Click, Double Click
✅ Scroll: Up and Down with proper delta values
✅ Real-time coordinate normalization
✅ Drag and drop support

## 🔧 How to Use in Your App

### Step 1: Import the Component

```jsx
import ApplicationControl from './pages/ApplicationControl';
```

### Step 2: Pass Required Props

```jsx
<ApplicationControl 
  socketRef={socketRef} 
  sessionId={sessionId}
/>
```

**Props:**
- `socketRef` (ref): WebSocket connection reference
- `sessionId` (string): Current session ID from the session

### Step 3: Example Integration in Receiver.jsx

```jsx
// In Receiver.jsx
import ApplicationControl from './pages/ApplicationControl';

function Receiver() {
  const [sessionId, setSessionId] = useState("");
  const socketRef = useRef(null);

  // ... existing code ...

  return (
    <div className="receiver-container">
      {connected && (
        <div className="control-panel">
          <ApplicationControl 
            socketRef={socketRef}
            sessionId={sessionId}
          />
        </div>
      )}
      {/* ... other components ... */}
    </div>
  );
}
```

## 📡 WebSocket Message Protocol

### Get Windows List
```json
{
  "type": "get-windows",
  "sessionId": "your-session-id",
  "data": {
    "search": "optional-search-term"
  }
}
```

### Control Application
```json
{
  "type": "control-app",
  "sessionId": "your-session-id",
  "data": {
    "action": "click|rightclick|doubleclick|scroll|drag|text|key|focus|minimize|maximize|close|resize",
    "normalizedX": 0.5,  // For mouse operations (0..1)
    "normalizedY": 0.5,
    "delta": 5,  // For scroll (positive=up, negative=down)
    "text": "Hello",  // For text action
    "keyCode": 0x41,  // For key action
    "ctrl": true,  // Optional modifier
    "hwnd": 12345,  // For window control
    "x": 100,  // For resize
    "y": 100,
    "width": 800,
    "height": 600
  }
}
```

## 🎮 Keyboard Virtual Key Codes Reference

```
0x08  = VK_BACK (Backspace)
0x09  = VK_TAB
0x0D  = VK_RETURN (Enter)
0x1B  = VK_ESCAPE
0x20  = VK_SPACE
0x25  = VK_LEFT
0x26  = VK_UP
0x27  = VK_RIGHT
0x28  = VK_DOWN
0x41  = A
0x43  = C
0x46  = F
0x53  = S
0x56  = V
0x58  = X
0x5A  = Z
0x59  = Y
0x70  = VK_F1
0x74  = VK_F5
0x7B  = VK_F12
```

## ⚙️ Desktop Application Requirements

The desktop sender must implement WebSocket handlers to:

1. **Process `get-windows` messages**
   - Call `ApplicationWindowManager.getAllVisibleWindows()`
   - Send back `windows-list` response with array of `WindowInfo`

2. **Process `control-app` messages**
   - Parse the control command
   - Call appropriate `ApplicationControlService` method
   - Send back `control-app-response` with success/failure status

## 📝 Example Desktop WebSocket Handler

```java
// In your WebSocket message handler
if ("get-windows".equals(payload.getType())) {
    List<ApplicationWindowManager.WindowInfo> windows = 
        ApplicationWindowManager.getAllVisibleWindows();
    
    // Send response back to client
    send(session, "windows-list", Map.of("windows", windows));
}

if ("control-app".equals(payload.getType())) {
    JsonNode data = payload.getData();
    String action = data.get("action").asText();
    
    switch(action) {
        case "focus":
            ApplicationControlService.focusApplication(data.get("hwnd").asLong());
            break;
        case "text":
            ApplicationControlService.sendTextViaClipboard(data.get("text").asText());
            break;
        case "click":
            ApplicationControlService.doubleClick(
                data.get("normalizedX").asDouble(),
                data.get("normalizedY").asDouble()
            );
            break;
        // ... handle other actions ...
    }
    
    send(session, "control-app-response", Map.of(
        "success", true,
        "message", "Command executed"
    ));
}
```

## 🧪 Testing

### Manual Test Steps:

1. **Start Backend**
   ```bash
   mvn -f backend spring-boot:run
   ```

2. **Start Desktop Sender**
   ```bash
   java -jar desktop/target/desktop-sender-1.0.0-shaded.jar
   ```

3. **Open Frontend**
   - Connect sender and receiver
   - Navigate to Application Control tab
   - Click "Refresh" to list windows
   - Try keyboard shortcuts
   - Click in mouse canvas to test mouse control

### Expected Results:
✅ Windows list updates with visible applications
✅ Keyboard presses send commands and show status
✅ Mouse clicks work in the canvas area
✅ Status bar shows real-time feedback

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Windows not listed | Ensure desktop sender is running and WebSocket is connected |
| Commands not executing | Check that session ID is passed to component |
| Keyboard not working | Verify virtual key codes are correct for your keyboard layout |
| Mouse not responding | Check normalization: coordinates should be 0..1 |
| Status not updating | Verify WebSocket message handler is receiving responses |

## 🔐 Security Notes

- ⚠️ This feature gives full control over the remote desktop
- Always use WSS (WebSocket Secure) in production
- Implement proper authentication and authorization
- Log all remote actions for audit trails
- Restrict access to trusted users only
- Consider implementing rate limiting for commands

## 🚀 Next Steps

1. Integrate component into your Receiver page
2. Test with Windows desktop sender
3. Monitor logs for any issues
4. Adjust CSS if needed for your UI theme
5. Consider adding macro recording/playback
6. Add screenshot functionality
7. Implement clipboard sync

---

**Status**: ✅ Ready for integration and testing
**Last Updated**: 2026-09-12
