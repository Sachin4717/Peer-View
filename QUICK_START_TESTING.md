# 🚀 Quick Start - Begin Testing NOW

## Currently Running Services

```
✅ Backend: http://localhost:5001
✅ Desktop Sender: Running (GUI application)
✅ Frontend: http://localhost:5174
```

---

## Quick Test (5 Minutes)

### 1. Open Browser (http://localhost:5174)

You'll see two tabs: **Sender** and **Receiver**

### 2. Click "Sender" Tab
- Should see connection status
- Note the Session ID

### 3. Click "Receiver" Tab
- Enter the Session ID
- Click Connect
- You should see remote screen appearing

### 4. Look for "Application Control" Panel
Should have 4 tabs: **Windows** | **Text** | **Keyboard** | **Mouse**

### 5. Test Each Feature (Just 30 seconds each)

#### Windows Tab ✓
1. Click "Refresh" button
2. Should see list of open windows
3. Click "Focus" on any window - it should come to front

#### Keyboard Tab ✓
1. Open Notepad on your desktop (the sender side)
2. Focus it using Windows tab (Focus button)
3. Click any keyboard button (e.g., "Ctrl+A")
4. Text should be selected in Notepad

#### Mouse Tab ✓
1. Click in the mouse canvas area
2. Remote cursor should move
3. Try clicking "Scroll Up" or "Scroll Down"
4. Webpage/document should scroll

#### Text Tab ✓
1. Focus Notepad using Windows tab
2. Type text in the textarea: "Hello Remote!"
3. Click "Send Text"
4. Text appears in Notepad

---

## 🎯 What Should Happen

| Action | Expected Result |
|--------|-----------------|
| Click "Refresh" Windows | List populates with open apps |
| Click "Focus" on window | Window comes to foreground |
| Click Ctrl+A keyboard button | Text in focused app selected |
| Click mouse canvas | Cursor moves to that position |
| Type text & send | Text appears in focused application |
| Click Scroll buttons | Page scrolls up/down |

---

## ⚠️ If Something Doesn't Work

Check desktop sender log:
1. Desktop sender window should show activity logs
2. Look for messages like:
   - "Click at (0.50, 0.50)"
   - "Key pressed: 0x41"
   - "Scroll at..."
   
If no messages appear, command isn't reaching desktop sender.

---

## 🔍 Browser Console Debug

Press F12 in browser and go to Console tab:

Should see WebSocket messages like:
```
Sending: {
  type: 'control-app',
  sessionId: 'abc123',
  data: { action: 'click', normalizedX: 0.5, normalizedY: 0.5 }
}
```

If you see errors, note them down.

---

## 📝 Test Execution Order

1. **Windows Management** - Focus/minimize/maximize windows
2. **Keyboard Input** - Type text, use shortcuts
3. **Mouse Control** - Click, scroll, drag
4. **Combined Test** - Perform multiple actions in sequence

---

## Quick Pass/Fail Criteria

✅ **PASS** if:
- Windows list shows applications
- Focused window comes to front
- Keyboard buttons work (text appears)
- Mouse clicks register
- Text input works
- Status bar updates with success/fail messages

❌ **FAIL** if:
- Commands don't execute
- No feedback in UI
- Desktop sender shows errors
- Window doesn't respond to commands

---

## 📞 Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| "No windows listed" | Click Refresh button again |
| "Keyboard not working" | Focus window first using Windows tab |
| "Mouse clicks disappear" | Check backend connection in desktop sender logs |
| "Nothing responds" | Restart desktop sender JAR |
| "WebSocket error" | Ensure backend is still running on 5001 |

---

## 🎬 Screen Recording for Documentation

If testing, consider recording:
1. Windows management (list, focus, minimize, close)
2. Keyboard input (typing, shortcuts)
3. Mouse control (click, drag, scroll)
4. Text input to different apps

---

**Ready? → Open http://localhost:5174 and start testing! 🚀**

---

For detailed testing guide, see: `COMPLETE_TESTING_GUIDE.md`
