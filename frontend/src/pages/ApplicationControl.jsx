import { useState, useEffect, useRef } from 'react';
import './ApplicationControl.css';

/**
 * ApplicationControl Component
 * Provides AnyDesk-like remote application control features:
 * - List and search for applications
 * - Focus, minimize, maximize, close windows
 * - Send text/keyboard input
 * - Mouse control (click, right-click, scroll, drag)
 */
function ApplicationControl({ socketRef, sessionId }) {
  const [windows, setWindows] = useState([]);
  const [selectedWindow, setSelectedWindow] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('Ready');
  const [textInput, setTextInput] = useState('');
  const [activeTab, setActiveTab] = useState('windows'); // windows, text, keyboard, mouse

  const canvasRef = useRef(null);
  const dragStartRef = useRef(null);
  const socketMessageHandlerRef = useRef(null);

  /**
   * Refresh the window list
   */
  const refreshWindows = () => {
    if (!sessionId) {
      setStatus('Error: No session ID');
      return;
    }

    setLoading(true);
    setStatus('Fetching windows...');
    
    if (socketRef?.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify({
        type: 'get-windows',
        sessionId: sessionId,
        data: { search: searchTerm }
      }));
    } else {
      setStatus('Error: Not connected to server');
      setLoading(false);
    }
  };

  /**
   * Send a command to the application
   */
  const sendCommand = (command) => {
    if (!sessionId) {
      setStatus('Error: No session ID');
      return;
    }

    if (!socketRef?.current || socketRef.current.readyState !== WebSocket.OPEN) {
      setStatus('Error: Not connected to server');
      return;
    }

    socketRef.current.send(JSON.stringify({
      type: 'control-app',
      sessionId: sessionId,
      data: command
    }));

    setStatus(`✓ Sent: ${command.action}`);
    setTimeout(() => setStatus('Ready'), 2000);
  };

  /**
   * Focus a window
   */
  const focusWindow = (hwnd) => {
    sendCommand({
      action: 'focus',
      hwnd: hwnd
    });
  };

  /**
   * Minimize a window
   */
  const minimizeWindow = (hwnd) => {
    sendCommand({
      action: 'minimize',
      hwnd: hwnd
    });
  };

  /**
   * Maximize a window
   */
  const maximizeWindow = (hwnd) => {
    sendCommand({
      action: 'maximize',
      hwnd: hwnd
    });
  };

  /**
   * Close a window
   */
  const closeWindow = (hwnd) => {
    sendCommand({
      action: 'close',
      hwnd: hwnd
    });
  };

  /**
   * Send text to the focused window
   */
  const sendText = () => {
    if (!textInput.trim()) {
      setStatus('Text cannot be empty');
      return;
    }

    sendCommand({
      action: 'text',
      text: textInput
    });

    setTextInput('');
  };

  /**
   * Send a key press
   */
  const sendKey = (keyCode, keyName) => {
    sendCommand({
      action: 'key',
      keyCode: keyCode
    });
    setStatus(`✓ ${keyName} pressed`);
  };

  /**
   * Send keyboard shortcut with Ctrl
   */
  const sendCtrlKey = (keyCode, keyName) => {
    sendCommand({
      action: 'key',
      keyCode: keyCode,
      ctrl: true
    });
    setStatus(`✓ Ctrl+${keyName}`);
  };

  /**
   * Click at position on screen
   */
  const click = (x, y, button = 'left') => {
    const normalizedX = x / window.innerWidth;
    const normalizedY = y / window.innerHeight;
    
    sendCommand({
      action: button === 'right' ? 'rightclick' : 'click',
      normalizedX: normalizedX,
      normalizedY: normalizedY
    });
  };

  /**
   * Double-click at position
   */
  const doubleClick = (x, y) => {
    const normalizedX = x / window.innerWidth;
    const normalizedY = y / window.innerHeight;
    
    sendCommand({
      action: 'doubleclick',
      normalizedX: normalizedX,
      normalizedY: normalizedY
    });
  };

  /**
   * Scroll at position
   */
  const scroll = (x, y, delta) => {
    const normalizedX = x / window.innerWidth;
    const normalizedY = y / window.innerHeight;
    
    sendCommand({
      action: 'scroll',
      normalizedX: normalizedX,
      normalizedY: normalizedY,
      delta: delta
    });
  };

  /**
   * Drag from one position to another
   */
  const startDrag = (x, y) => {
    dragStartRef.current = { x, y };
  };

  const endDrag = (x, y) => {
    if (dragStartRef.current) {
      sendCommand({
        action: 'drag',
        fromX: dragStartRef.current.x / window.innerWidth,
        fromY: dragStartRef.current.y / window.innerHeight,
        toX: x / window.innerWidth,
        toY: y / window.innerHeight,
        duration: 500
      });
      dragStartRef.current = null;
    }
  };

  /**
   * Handle canvas mouse events for remote control
   */
  const handleCanvasClick = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    if (e.button === 0) click(x, y, 'left');
    else if (e.button === 2) click(x, y, 'right');
  };

  const handleCanvasDoubleClick = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    doubleClick(x, y);
  };

  const handleCanvasMouseDown = (e) => {
    if (e.button === 0) {
      const rect = canvasRef.current.getBoundingClientRect();
      startDrag(e.clientX - rect.left, e.clientY - rect.top);
    }
  };

  const handleCanvasMouseUp = (e) => {
    if (dragStartRef.current) {
      const rect = canvasRef.current.getBoundingClientRect();
      endDrag(e.clientX - rect.left, e.clientY - rect.top);
    }
  };

  const handleCanvasWheel = (e) => {
    e.preventDefault();
    const rect = canvasRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    scroll(x, y, e.deltaY > 0 ? -5 : 5);
  };

  // Listen for window updates from WebSocket
  useEffect(() => {
    if (!socketRef?.current) return;

    const handleMessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        // Handle windows-list response
        if (message.type === 'windows-list' && message.data) {
          setWindows(message.data.windows || []);
          setLoading(false);
          setStatus(`✓ Found ${(message.data.windows || []).length} windows`);
        }
        
        // Handle control-app response
        if (message.type === 'control-app-response' && message.data) {
          if (message.data.success) {
            setStatus(`✓ ${message.data.message || 'Command executed'}`);
          } else {
            setStatus(`✗ ${message.data.message || 'Command failed'}`);
          }
        }
      } catch (e) {
        // Silently ignore non-JSON messages
      }
    };

    socketRef.current.addEventListener('message', handleMessage);
    socketMessageHandlerRef.current = handleMessage;
    
    return () => {
      if (socketRef?.current && socketMessageHandlerRef.current) {
        socketRef.current.removeEventListener('message', socketMessageHandlerRef.current);
      }
    };
  }, [socketRef]);

  return (
    <div className="application-control-panel">
      <div className="control-header">
        <h2>Application Control</h2>
        <div className="status-bar">{status}</div>
      </div>

      <div className="control-tabs">
        <button 
          className={`tab-button ${activeTab === 'windows' ? 'active' : ''}`}
          onClick={() => setActiveTab('windows')}
        >
          Windows
        </button>
        <button 
          className={`tab-button ${activeTab === 'text' ? 'active' : ''}`}
          onClick={() => setActiveTab('text')}
        >
          Text Input
        </button>
        <button 
          className={`tab-button ${activeTab === 'keyboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('keyboard')}
        >
          Keyboard
        </button>
        <button 
          className={`tab-button ${activeTab === 'mouse' ? 'active' : ''}`}
          onClick={() => setActiveTab('mouse')}
        >
          Mouse Control
        </button>
      </div>

      {/* Windows Tab */}
      {activeTab === 'windows' && (
        <div className="tab-content">
          <div className="search-container">
            <input
              type="text"
              placeholder="Search windows..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            <button 
              onClick={refreshWindows}
              disabled={loading}
              className="refresh-button"
            >
              {loading ? 'Loading...' : 'Refresh'}
            </button>
          </div>

          <div className="windows-list">
            {windows.length === 0 ? (
              <p className="empty-message">No windows found. Click Refresh to list.</p>
            ) : (
              windows.map((window) => (
                <div 
                  key={window.hwnd} 
                  className={`window-item ${selectedWindow?.hwnd === window.hwnd ? 'selected' : ''}`}
                  onClick={() => setSelectedWindow(window)}
                >
                  <div className="window-title">{window.title}</div>
                  <div className="window-class">{window.className}</div>
                  <div className="window-info">
                    PID: {window.processId} | Position: ({window.x}, {window.y}) | Size: {window.width}x{window.height}
                  </div>
                  <div className="window-actions">
                    <button onClick={() => focusWindow(window.hwnd)} className="action-btn focus">
                      Focus
                    </button>
                    <button onClick={() => maximizeWindow(window.hwnd)} className="action-btn maximize">
                      Maximize
                    </button>
                    <button onClick={() => minimizeWindow(window.hwnd)} className="action-btn minimize">
                      Minimize
                    </button>
                    <button onClick={() => closeWindow(window.hwnd)} className="action-btn close">
                      Close
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Text Input Tab */}
      {activeTab === 'text' && (
        <div className="tab-content">
          <div className="text-input-container">
            <textarea
              value={textInput}
              onChange={(e) => setTextInput(e.target.value)}
              placeholder="Enter text to send to the remote application..."
              className="text-input"
              rows="6"
            />
            <div className="input-options">
              <label>
                <input type="checkbox" /> Use Clipboard (more reliable for large text)
              </label>
            </div>
            <button onClick={sendText} className="send-button">
              Send Text
            </button>
          </div>
        </div>
      )}

      {/* Keyboard Tab */}
      {activeTab === 'keyboard' && (
        <div className="tab-content">
          <div className="keyboard-grid">
            <div className="key-group">
              <h3>Function Keys</h3>
              <div className="key-buttons">
                <button onClick={() => sendKey(0x70, 'F1')} className="key-btn">F1</button>
                <button onClick={() => sendKey(0x74, 'F5')} className="key-btn">F5</button>
                <button onClick={() => sendKey(0x7B, 'F12')} className="key-btn">F12</button>
              </div>
            </div>

            <div className="key-group">
              <h3>Navigation</h3>
              <div className="key-buttons">
                <button onClick={() => sendKey(0x09, 'Tab')} className="key-btn">Tab</button>
                <button onClick={() => sendKey(0x0D, 'Enter')} className="key-btn">Enter</button>
                <button onClick={() => sendKey(0x08, 'Backspace')} className="key-btn">Backspace</button>
                <button onClick={() => sendKey(0x1B, 'Escape')} className="key-btn">Escape</button>
              </div>
            </div>

            <div className="key-group">
              <h3>Common Shortcuts</h3>
              <div className="key-buttons">
                <button onClick={() => sendCtrlKey(0x41, 'A')} className="key-btn">Ctrl+A (All)</button>
                <button onClick={() => sendCtrlKey(0x43, 'C')} className="key-btn">Ctrl+C (Copy)</button>
                <button onClick={() => sendCtrlKey(0x56, 'V')} className="key-btn">Ctrl+V (Paste)</button>
                <button onClick={() => sendCtrlKey(0x5A, 'Z')} className="key-btn">Ctrl+Z (Undo)</button>
                <button onClick={() => sendCtrlKey(0x59, 'Y')} className="key-btn">Ctrl+Y (Redo)</button>
                <button onClick={() => sendCtrlKey(0x58, 'X')} className="key-btn">Ctrl+X (Cut)</button>
                <button onClick={() => sendCtrlKey(0x53, 'S')} className="key-btn">Ctrl+S (Save)</button>
                <button onClick={() => sendCtrlKey(0x46, 'F')} className="key-btn">Ctrl+F (Find)</button>
              </div>
            </div>

            <div className="key-group">
              <h3>Arrow Keys</h3>
              <div className="key-buttons">
                <button onClick={() => sendKey(0x26, '↑')} className="key-btn">↑ Up</button>
                <button onClick={() => sendKey(0x28, '↓')} className="key-btn">↓ Down</button>
                <button onClick={() => sendKey(0x25, '←')} className="key-btn">← Left</button>
                <button onClick={() => sendKey(0x27, '→')} className="key-btn">→ Right</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Mouse Control Tab */}
      {activeTab === 'mouse' && (
        <div className="tab-content">
          <div className="mouse-control">
            <p className="mouse-info">
              💡 Click in the canvas area to control remote mouse. Drag to move, scroll wheel to scroll. Right-click for context menu.
            </p>
            <div 
              ref={canvasRef}
              className="mouse-canvas"
              onClick={handleCanvasClick}
              onDoubleClick={handleCanvasDoubleClick}
              onMouseDown={handleCanvasMouseDown}
              onMouseUp={handleCanvasMouseUp}
              onWheel={handleCanvasWheel}
              onContextMenu={(e) => e.preventDefault()}
            >
              <div className="canvas-content">
                <div className="canvas-icon">🖱️</div>
                <p className="canvas-placeholder">Click to control remote mouse</p>
              </div>
            </div>
            <div className="mouse-buttons">
              <button 
                className="mouse-btn left-click"
                onMouseDown={() => sendCommand({ action: 'click', normalizedX: 0.5, normalizedY: 0.5 })}
              >
                🖱️ Left Click
              </button>
              <button 
                className="mouse-btn right-click"
                onMouseDown={() => sendCommand({ action: 'rightclick', normalizedX: 0.5, normalizedY: 0.5 })}
              >
                🖱️ Right Click
              </button>
              <button 
                className="mouse-btn double-click"
                onClick={() => sendCommand({ action: 'doubleclick', normalizedX: 0.5, normalizedY: 0.5 })}
              >
                🖱️ Double Click
              </button>
              <button 
                className="mouse-btn scroll-up"
                onClick={() => sendCommand({ action: 'scroll', normalizedX: 0.5, normalizedY: 0.5, delta: 5 })}
              >
                ⬆️ Scroll Up
              </button>
              <button 
                className="mouse-btn scroll-down"
                onClick={() => sendCommand({ action: 'scroll', normalizedX: 0.5, normalizedY: 0.5, delta: -5 })}
              >
                ⬇️ Scroll Down
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ApplicationControl;
