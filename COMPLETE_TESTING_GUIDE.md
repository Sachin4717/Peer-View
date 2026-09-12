# 🧪 Complete End-to-End Testing Guide - Remote Control Features

## 🚀 Services Status

✅ **Backend**: Running on http://localhost:5001
✅ **Desktop Sender**: GUI application running
✅ **Frontend**: Running on http://localhost:5174

---

## 📋 Test Plan Overview

This guide covers testing all remote control features:
1. Window Management (list, focus, minimize, maximize, close)
2. Keyboard Control (function keys, navigation, shortcuts, arrows)
3. Mouse Control (click, drag, scroll, right-click, double-click)
4. Text Input (send text to any application)

---

## 🎬 Test Execution Steps

### Step 1: Open Frontend and Create Session

1. Open browser: http://localhost:5174
2. You should see two tabs: **Sender** and **Receiver**
3. Click on **Sender** tab first
4. You should see a message like: "Waiting for connection on ws://localhost:5001/ws"
5. Look for a session ID that will be displayed
6. Copy the Session ID (you'll need it)

**Expected Result**: ✅ Sender connected, session ID visible

---

### Step 2: Connect Receiver to Sender

1. Still in browser, click on **Receiver** tab
2. Paste or enter the Session ID from Sender
3. Click "Connect" button
4. Desktop sender window should show: "Receiver connected. Sharing screen..."

**Expected Result**: ✅ Receiver sees remote screen, desktop sender shows connected status

---

### Step 3: Open Application Control Panel

1. In Receiver tab, look for navigation tabs (Windows, Text, Keyboard, Mouse)
2. You should see 4 tabs in the Application Control panel
3. Start with **Windows** tab (should be active by default)

**Expected Result**: ✅ Application Control panel visible with 4 tabs

---

## 🪟 Test 1: Windows Management

### 1.1 Refresh Windows List

**Steps:**
1. Click "Refresh" button in Windows tab
2. Wait for list to populate
3. You should see a list of open applications with titles

**Expected Result:**
- ✅ List populates with window titles
- ✅ Status shows "Found N windows"
- ✅ Each window shows: title, class name, position, size
- ✅ Can see desktop sender window in list

**Test Output Example:**
```
Notepad - Untitled
Google Chrome
Visual Studio Code
RemoteSupport - Desktop Sender
```

### 1.2 Focus Window

**Steps:**
1. Select a window from the list (e.g., Notepad)
2. Click "Focus" button
3. On your desktop, that window should come to foreground
4. Verify status shows "✓ Focused window: [hwnd]"

**Expected Result:**
- ✅ Clicked window appears in foreground
- ✅ Status bar shows success message
- ✅ Can type immediately in focused window

**Test Command:** Open Notepad, focus it from receiver

### 1.3 Minimize Window

**Steps:**
1. Select a window from list
2. Click "Minimize" button
3. Window should minimize to taskbar
4. Status shows success

**Expected Result:**
- ✅ Window minimizes
- ✅ No longer visible on screen
- ✅ Status bar confirmation

### 1.4 Maximize Window

**Steps:**
1. Select a minimized or normal window
2. Click "Maximize" button
3. Window should maximize to fill screen
4. Status shows success

**Expected Result:**
- ✅ Window maximizes to screen size
- ✅ Status bar shows confirmation
- ✅ Window covers full screen

### 1.5 Close Window

**Steps:**
1. Open a test application (e.g., Notepad)
2. Refresh windows list
3. Select the application
4. Click "Close" button
5. Window should close gracefully

**Expected Result:**
- ✅ Window closes without error
- ✅ Application terminates
- ✅ No crashes or exceptions

---

## ⌨️ Test 2: Keyboard Control

### 2.1 Function Keys

**Steps:**
1. Click "Keyboard" tab
2. Open Notepad
3. Focus Notepad using Windows tab
4. In Keyboard tab, click "F1" button
5. Help dialog should appear (or system help)

**Expected Result:**
- ✅ F1 key registers in focused app
- ✅ App responds to function key press
- ✅ No delays or multiple presses

**Test Cases:**
- F1: Open Help
- F5: Refresh (in browsers)
- F12: Developer tools (in browsers)

### 2.2 Navigation Keys

**Steps:**
1. Open a text editor (Notepad)
2. Focus it
3. Click "Tab" button
4. Tab character should be inserted
5. Click "Enter" button
6. New line created

**Expected Result:**
- ✅ Tab key inserts tab character
- ✅ Enter key creates new line
- ✅ Backspace deletes character
- ✅ Escape closes dialogs

### 2.3 Keyboard Shortcuts

**Steps:**
1. Open Notepad
2. Focus it
3. In Keyboard tab, click "Ctrl+A"
4. Text "Select All" should activate
5. Click "Ctrl+C" to copy (if text selected)
6. Click "Ctrl+V" to paste

**Expected Result:**
- ✅ Ctrl+A selects all content
- ✅ Ctrl+C copies to clipboard
- ✅ Ctrl+V pastes content
- ✅ Ctrl+Z undoes last action
- ✅ Ctrl+X cuts content
- ✅ Ctrl+S saves file
- ✅ Ctrl+F opens find dialog

**Comprehensive Shortcut Test:**

| Shortcut | App | Expected Action |
|----------|-----|-----------------|
| Ctrl+A | Notepad | Select all text |
| Ctrl+C | Notepad | Copy selected text |
| Ctrl+V | Notepad | Paste copied text |
| Ctrl+Z | Notepad | Undo last action |
| Ctrl+Y | Notepad | Redo action |
| Ctrl+X | Notepad | Cut selected text |
| Ctrl+S | Notepad | Save file |
| Ctrl+F | Chrome | Open find in page |

### 2.4 Arrow Keys

**Steps:**
1. Open Notepad
2. Type some text
3. Click "↑ Up" button
4. Cursor should move up one line
5. Click "→ Right" button
6. Cursor moves right one character

**Expected Result:**
- ✅ Up arrow moves cursor up
- ✅ Down arrow moves cursor down
- ✅ Left arrow moves cursor left
- ✅ Right arrow moves cursor right

---

## 🖱️ Test 3: Mouse Control

### 3.1 Click at Position

**Steps:**
1. Click "Mouse" tab
2. Open Notepad
3. Focus it using Windows tab
4. Click in different areas of the mouse canvas
5. Each click should translate to remote cursor position

**Expected Result:**
- ✅ Cursor moves to clicked position on remote screen
- ✅ Click registers (text insertion point changes)
- ✅ Coordinates normalize correctly (0..1 range)

### 3.2 Right-Click (Context Menu)

**Steps:**
1. In mouse canvas area
2. Right-click in different areas
3. Context menus should appear
4. Verify correct menu for location

**Expected Result:**
- ✅ Right-click produces context menu
- ✅ Menu appears at clicked location
- ✅ Can select menu items with left-click

### 3.3 Double-Click

**Steps:**
1. In Notepad with text
2. Click "Double Click" button in mouse area
3. Near a word
4. Word should be selected (double-click selects word)

**Expected Result:**
- ✅ Double-click selects word in text
- ✅ Two sequential clicks register

### 3.4 Drag and Drop

**Steps:**
1. In mouse canvas area
2. Click and drag from one point to another
3. Drag should select text or move objects
4. Release mouse

**Expected Result:**
- ✅ Drag motion smooth
- ✅ Selection or movement occurs
- ✅ Drop position correct

### 3.5 Scroll Wheel

**Steps:**
1. Open webpage or document
2. Click "↑ Scroll Up" button
3. Page scrolls up
4. Click "↓ Scroll Down" button
5. Page scrolls down

**Expected Result:**
- ✅ Scroll Up scrolls page upward
- ✅ Scroll Down scrolls page downward
- ✅ Scrolling is smooth
- ✅ Correct delta applied

**Test in Different Applications:**
- Notepad: Scroll through document
- Web Browser: Scroll through webpage
- File Explorer: Scroll through file list

---

## 📝 Test 4: Text Input

### 4.1 Simple Text Input

**Steps:**
1. Click "Text Input" tab
2. Open Notepad
3. Focus Notepad
4. Type: "Hello from Remote Control!"
5. Click "Send Text"

**Expected Result:**
- ✅ Text appears in Notepad
- ✅ Status shows: "✓ Sent text"
- ✅ Text is complete and accurate

### 4.2 Special Characters

**Steps:**
1. In text input, enter: "Test!@#$%^&*()"
2. Click "Send Text"
3. Text should appear exactly in remote app

**Expected Result:**
- ✅ Special characters preserved
- ✅ No character mapping errors
- ✅ Exact reproduction

### 4.3 Multiline Text

**Steps:**
1. In text input textarea
2. Type:
   ```
   Line 1
   Line 2
   Line 3
   ```
3. Click "Send Text"

**Expected Result:**
- ✅ Multiple lines inserted
- ✅ Line breaks preserved
- ✅ Formatting maintained

### 4.4 Unicode/Emoji

**Steps:**
1. Type: "Unicode test: 你好 🚀 Привет"
2. Click "Send Text"

**Expected Result:**
- ✅ Unicode characters preserved
- ✅ Emoji displays correctly
- ✅ International characters work

---

## 🔄 Test 5: Combined Operations

### Scenario 1: Document Editing

**Steps:**
1. Open Notepad on sender screen
2. From receiver:
   - Focus Notepad using Windows tab
   - Send text: "This is a test document"
   - Use Ctrl+A to select all
   - Use Ctrl+B to bold (if supported)
   - Use Ctrl+S to save

**Expected Result:**
- ✅ All operations complete
- ✅ Document modified correctly
- ✅ Save successful

### Scenario 2: Web Navigation

**Steps:**
1. Open Chrome/Edge browser
2. From receiver:
   - Focus browser using Windows tab
   - Use Ctrl+L to focus address bar
   - Send text: "google.com"
   - Press Enter key
   - Use Ctrl+F to open find
   - Send text: "test search"
   - Scroll up/down to navigate

**Expected Result:**
- ✅ Browser focuses and navigates
- ✅ Search works
- ✅ Scrolling functions
- ✅ Find dialog opens and searches

### Scenario 3: Multi-Window Control

**Steps:**
1. Open 3+ windows (Notepad, Chrome, File Explorer)
2. From receiver:
   - Refresh windows list
   - Switch between windows using Focus button
   - For each window:
     - Click in mouse canvas
     - Type text
     - Use keyboard shortcuts
   - Minimize each window after use

**Expected Result:**
- ✅ Fast switching between windows
- ✅ Each window responds correctly
- ✅ No cross-window interference
- ✅ All windows minimize properly

---

## 📊 Test Results Template

Use this template to document test results:

```
Test Name: ___________________
Date: _______________________
Browser: ____________________
OS: _________________________

✅ PASS / ❌ FAIL / ⚠️ PARTIAL

Test Steps:
1. _________________________
2. _________________________
3. _________________________

Expected Result: ____________
Actual Result: ______________

Notes/Issues: _______________
_____________________________
```

---

## 🐛 Troubleshooting

### Issue: Windows list is empty
**Solution:**
- Refresh button again
- Ensure windows are visible (not all minimized)
- Check desktop sender logs for errors
- Restart desktop sender if needed

### Issue: Keyboard input not working
**Solution:**
- Ensure window has focus (use Focus button first)
- Check that desktop sender window is not in focus
- Verify keyboard is working locally
- Try pressing Tab or Enter first to test

### Issue: Mouse clicks not registering
**Solution:**
- Click in center of mouse canvas first
- Verify coordinates are normalizing (0..1 range)
- Try clicking different areas of the canvas
- Check if remote cursor is visible

### Issue: Text not appearing
**Solution:**
- Focus window first using Windows tab
- Try small text first ("Hi")
- Verify window is editable
- Check clipboard operations
- Try typing one character at a time

### Issue: Scroll not working
**Solution:**
- Ensure mouse is positioned over scrollable area
- Try scrolling in center of window
- Verify scroll delta values in WebSocket messages
- Test in Chrome, Notepad, and File Explorer

### Issue: Status bar not updating
**Solution:**
- Check browser console for errors
- Verify WebSocket connection is open
- Check that sessionId is being sent correctly
- Reload page if stuck

---

## ✅ Final Verification Checklist

Before considering the feature complete, verify:

- [ ] Windows Management (list, focus, minimize, maximize, close)
- [ ] Keyboard Tab (all key groups working)
- [ ] Mouse Control (click, drag, scroll, right-click)
- [ ] Text Input (simple, special chars, multiline, unicode)
- [ ] Combined scenarios (editing, navigation, multi-window)
- [ ] Status feedback in real-time
- [ ] No crashes or exceptions
- [ ] All browsers tested (Chrome, Firefox, Edge)
- [ ] Multi-monitor coordinate handling works
- [ ] Performance acceptable
- [ ] Desktop sender logs clean
- [ ] Frontend console clean (no errors)
- [ ] Backend logs show proper message handling

---

## 📈 Performance Metrics to Monitor

While testing, note:
- **Latency**: Time between click and remote response
- **Throughput**: Number of commands per second
- **Reliability**: Commands that work vs fail percentage
- **CPU Usage**: Both sender and receiver
- **Memory Usage**: Frontend memory consumption
- **Message Size**: WebSocket payload sizes

---

## 🎯 Success Criteria

The feature is complete when:

1. ✅ All windows management operations work
2. ✅ All keyboard keys register correctly
3. ✅ All mouse operations work with normalized coordinates
4. ✅ Text input works for all character types
5. ✅ Status updates appear in real-time
6. ✅ No crashes or errors in logs
7. ✅ Combined scenarios execute smoothly
8. ✅ Multi-monitor support works
9. ✅ Performance is acceptable (<500ms latency)
10. ✅ User experience is smooth and intuitive

---

**Test Execution Date**: ________________
**Tester Name**: ________________________
**Overall Result**: ✅ PASS / ❌ FAIL

---

**Last Updated**: 2026-09-12
