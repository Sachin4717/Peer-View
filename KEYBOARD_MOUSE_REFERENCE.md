# Quick Reference Guide - Application Control

## 🎮 Keyboard Virtual Key Codes

### Control Keys
```
0x08  = VK_BACK        (Backspace)
0x09  = VK_TAB         (Tab)
0x0D  = VK_RETURN      (Enter)
0x1B  = VK_ESCAPE      (Escape)
0x20  = VK_SPACE       (Space)
0x2C  = VK_SNAPSHOT    (Print Screen)
0x2D  = VK_INSERT      (Insert)
0x2E  = VK_DELETE      (Delete)
0x23  = VK_END         (End)
0x24  = VK_HOME        (Home)
0x21  = VK_PRIOR       (Page Up)
0x22  = VK_NEXT        (Page Down)
```

### Arrow Keys
```
0x25  = VK_LEFT        (←)
0x26  = VK_UP          (↑)
0x27  = VK_RIGHT       (→)
0x28  = VK_DOWN        (↓)
```

### Function Keys
```
0x70  = VK_F1
0x71  = VK_F2
0x72  = VK_F3
0x73  = VK_F4
0x74  = VK_F5
0x75  = VK_F6
0x76  = VK_F7
0x77  = VK_F8
0x78  = VK_F9
0x79  = VK_F10
0x7A  = VK_F11
0x7B  = VK_F12
```

### Modifier Keys
```
0x10  = VK_SHIFT       (Shift)
0x11  = VK_CONTROL     (Ctrl)
0x12  = VK_MENU        (Alt)
```

### Letter Keys (A-Z)
```
0x41  = 'A'  /  0x61  = 'a'
0x42  = 'B'  /  0x62  = 'b'
... (pattern continues)
0x5A  = 'Z'  /  0x7A  = 'z'
```

### Number Keys (0-9)
```
0x30  = '0'
0x31  = '1'
0x32  = '2'
0x33  = '3'
0x34  = '4'
0x35  = '5'
0x36  = '6'
0x37  = '7'
0x38  = '8'
0x39  = '9'
```

### Special Characters
```
0xBA  = ';:'      (Semicolon/Colon)
0xBB  = '=+'      (Equals/Plus)
0xBC  = ',<'      (Comma/Less)
0xBD  = '-_'      (Minus/Underscore)
0xBE  = '.>'      (Period/Greater)
0xBF  = '/?'      (Slash/Question)
0xC0  = '`~'      (Backtick/Tilde)
0xDB  = '[{'      (Left Bracket/Brace)
0xDC  = '\|'      (Backslash/Pipe)
0xDD  = ']}'      (Right Bracket/Brace)
0xDE  = '"\''     (Quote/Apostrophe)
```

### Numpad Keys
```
0x60  = VK_NUMPAD0
0x61  = VK_NUMPAD1
... (pattern continues)
0x69  = VK_NUMPAD9
0x6A  = VK_MULTIPLY    (*)
0x6B  = VK_ADD         (+)
0x6D  = VK_SUBTRACT    (-)
0x6E  = VK_DECIMAL     (.)
0x6F  = VK_DIVIDE      (/)
```

---

## 📡 WebSocket Command Examples

### Send a Simple Key Press

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'key',
    keyCode: 0x0D  // Enter key
  }
}));
```

### Send Keyboard Shortcut (Ctrl+C)

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'key',
    keyCode: 0x43,  // 'C'
    ctrl: true      // With Ctrl modifier
  }
}));
```

### Send Text

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'text',
    text: 'Hello, World!'
  }
}));
```

### Click at Position

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'click',
    normalizedX: 0.5,  // Center of screen (0..1)
    normalizedY: 0.5
  }
}));
```

### Right-Click at Position

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'rightclick',
    normalizedX: 0.3,
    normalizedY: 0.4
  }
}));
```

### Double-Click

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'doubleclick',
    normalizedX: 0.5,
    normalizedY: 0.5
  }
}));
```

### Scroll

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'scroll',
    normalizedX: 0.5,
    normalizedY: 0.5,
    delta: 5  // Positive = up, Negative = down
  }
}));
```

### Drag Mouse

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'drag',
    fromX: 0.2,
    fromY: 0.2,
    toX: 0.8,
    toY: 0.8,
    duration: 500  // milliseconds
  }
}));
```

### Focus Window

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'focus',
    hwnd: 12345678  // Window handle from windows list
  }
}));
```

### Maximize Window

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'maximize',
    hwnd: 12345678
  }
}));
```

### Minimize Window

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'minimize',
    hwnd: 12345678
  }
}));
```

### Close Window

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'close',
    hwnd: 12345678
  }
}));
```

### Resize Window

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'resize',
    hwnd: 12345678,
    x: 100,       // X position
    y: 100,       // Y position
    width: 800,   // Width
    height: 600   // Height
  }
}));
```

### Get Windows List

```javascript
socket.send(JSON.stringify({
  type: 'get-windows',
  sessionId: 'abc123',
  data: {
    search: 'notepad'  // Optional search term
  }
}));
```

---

## 🎯 Common Use Cases

### Copy-Paste Workflow

```javascript
// Select all
socket.send(...{ action: 'key', keyCode: 0x41, ctrl: true });

// Copy
setTimeout(() => {
  socket.send(...{ action: 'key', keyCode: 0x43, ctrl: true });
}, 100);

// Type new content
setTimeout(() => {
  socket.send(...{ action: 'text', text: 'New content' });
}, 200);

// Paste
setTimeout(() => {
  socket.send(...{ action: 'key', keyCode: 0x56, ctrl: true });
}, 300);
```

### Open Run Dialog (Windows: Win+R)

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'key',
    keyCode: 0x91,  // VK_LWIN (Windows key)
    shift: true     // Actually, hold R key separately
  }
}));
```

### Alt+Tab (Switch Windows)

```javascript
socket.send(JSON.stringify({
  type: 'control-app',
  sessionId: 'abc123',
  data: {
    action: 'key',
    keyCode: 0x09,  // Tab
    alt: true       // With Alt modifier
  }
}));
```

### Triple-Click to Select Line

```javascript
// Click at start
socket.send(...{ action: 'click', normalizedX: 0.2, normalizedY: 0.3 });

setTimeout(() => {
  // Triple click to select entire line
  socket.send(...{ action: 'tripleclick', normalizedX: 0.2, normalizedY: 0.3 });
}, 100);
```

---

## 📊 Coordinate System

The coordinate system uses **normalized coordinates** (0..1) to work across multi-monitor setups:

```
(0, 0) ─────────────────────── (1, 0)
  │                               │
  │    (0.5, 0.5) = Center        │
  │                               │
  │                               │
(0, 1) ─────────────────────── (1, 1)

Example:
- Center of screen: normalizedX=0.5, normalizedY=0.5
- Top-left: normalizedX=0.0, normalizedY=0.0
- Bottom-right: normalizedX=1.0, normalizedY=1.0
```

---

## 🔧 Component Props

```jsx
<ApplicationControl
  socketRef={ref}      // WebSocket connection reference
  sessionId={string}   // Current session ID (required)
/>
```

---

## 📋 Supported Actions

| Action | Required Fields | Optional Fields |
|--------|-----------------|-----------------|
| `key` | keyCode | ctrl, shift, alt |
| `text` | text | - |
| `click` | normalizedX, normalizedY | - |
| `rightclick` | normalizedX, normalizedY | - |
| `doubleclick` | normalizedX, normalizedY | - |
| `scroll` | normalizedX, normalizedY, delta | - |
| `drag` | fromX, fromY, toX, toY | duration |
| `focus` | hwnd | - |
| `minimize` | hwnd | - |
| `maximize` | hwnd | - |
| `close` | hwnd | - |
| `resize` | hwnd, x, y, width, height | - |

---

## 💡 Tips & Tricks

1. **Use Clipboard for Large Text**: For text longer than 1KB, use clipboard:
   ```javascript
   { action: 'text', text: largeText, useClipboard: true }
   ```

2. **Detect Remote Cursor Position**: Mouse coordinates are normalized, convert them:
   ```javascript
   const screenX = normalizedX * window.innerWidth;
   const screenY = normalizedY * window.innerHeight;
   ```

3. **Multi-Monitor Support**: Normalized coordinates automatically handle multiple monitors

4. **Keyboard Layout Independence**: Virtual key codes work regardless of keyboard layout

5. **Batch Commands**: Send multiple commands quickly using setTimeout for sequencing

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| Keys not registering | Ensure desktop app has focus |
| Mouse not moving | Check normalized coordinates (0..1) |
| Text not appearing | Try clipboard method instead |
| Window not found | Use search term to filter windows |
| Coordinate offset | Ensure canvas element position is correct |

---

**Last Updated**: 2026-09-12
**Version**: 1.0.0
