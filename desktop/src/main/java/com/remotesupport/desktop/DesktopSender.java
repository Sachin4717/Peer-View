package com.remotesupport.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jna.platform.win32.WinDef;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Native Windows desktop sender.
 *
 * <p>
 * Replaces the browser-based sender for full remote support:
 * <ul>
 * <li>Streams the real screen (all monitors) to the receiver.</li>
 * <li>Injects receiver mouse/keyboard input at the OS level (SendInput).</li>
 * <li>Can exclude its own window from screen capture
 * (WDA_EXCLUDEFROMCAPTURE).</li>
 * <li>Supports file upload (receiver -> Downloads) and download.</li>
 * </ul>
 */
public class DesktopSender extends JFrame {

    private static final String WS_URL = "ws://localhost:5001/ws";
    private static final int CHUNK_SIZE = 256 * 1024; // bytes per file chunk
    private static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JTextArea log = new JTextArea(12, 46);
    private final JLabel sessionLabel = new JLabel("Session: -");
    private final JLabel statusLabel = new JLabel("Status: connecting...");
    private final JCheckBox hideFromCapture = new JCheckBox("Hide this window from screen capture");
    private final JCheckBox allowSharing = new JCheckBox("Share my screen when a receiver connects", true);

    private WsClient ws;
    private String sessionId;
    private boolean receiverConnected;
    private volatile boolean mouseControlGranted;
    private volatile boolean keyboardControlGranted;
    private volatile boolean audioFromReceiverEnabled;
    private ScreenCaptureThread captureThread;
    private final ConcurrentHashMap<String, Path> downloads = new ConcurrentHashMap<>();

    public DesktopSender() {
        super("RemoteSupport - Desktop Sender");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        buildUi();
        connect();
    }

    private void buildUi() {
        JPanel panel = new JPanel(new java.awt.BorderLayout(8, 8));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new java.awt.GridLayout(0, 1));
        top.add(sessionLabel);
        top.add(statusLabel);
        top.add(allowSharing);
        top.add(hideFromCapture);
        hideFromCapture.addActionListener(e -> applyCaptureExclusion());

        JButton shareButton = new JButton(receiverConnected ? "Stop sharing" : "Share session with receiver");
        JButton copyButton = new JButton("Copy session ID");
        copyButton.addActionListener(e -> {
            var clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new java.awt.datatransfer.StringSelection(sessionId == null ? "" : sessionId), null);
            appendLog("Session ID copied.");
        });
        JButton sendFileButton = new JButton("Send a file to the receiver...");
        sendFileButton.addActionListener(e -> sendFileToReceiver());

        JPanel buttons = new JPanel();
        buttons.add(shareButton);
        buttons.add(copyButton);
        buttons.add(sendFileButton);

        log.setEditable(false);
        panel.add(top, java.awt.BorderLayout.NORTH);
        panel.add(buttons, java.awt.BorderLayout.CENTER);
        panel.add(new JScrollPane(log), java.awt.BorderLayout.SOUTH);

        add(panel);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
                System.exit(0);
            }
        });
    }

    private void connect() {
        try {
            ws = new WsClient(new URI(WS_URL));
            ws.connect();
        } catch (Exception e) {
            appendLog("Connection failed: " + e.getMessage());
        }
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            log.append(line + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    // ------------------------------------------------------------------
    // Outgoing messages
    // ------------------------------------------------------------------

    private void send(ObjectNode node) {
        if (ws != null && ws.isOpen()) {
            ws.send(node.toString());
        }
    }

    private void sendSessionIdUpdate() {
        SwingUtilities.invokeLater(() -> sessionLabel.setText("Session: " + (sessionId == null ? "-" : sessionId)));
    }

    private void startSharing() {
        if (!receiverConnected || sessionId == null) {
            appendLog("No receiver connected yet.");
            return;
        }
        if (captureThread == null) {
            captureThread = new ScreenCaptureThread(frame -> {
                ObjectNode msg = mapper.createObjectNode();
                msg.put("type", "screen-frame");
                msg.put("sessionId", sessionId);
                msg.put("data", frame);
                send(msg);
            });
            captureThread.setQuality(0.55, 120);
            captureThread.start();
            appendLog("Screen sharing started.");
            applyCaptureExclusion();
        }
    }

    private void stopSharing() {
        if (captureThread != null) {
            captureThread.shutdown();
            captureThread = null;
            appendLog("Screen sharing stopped.");
        }
    }

    private void applyCaptureExclusion() {
        if (!InputInjector.isWindows()) {
            appendLog("Capture exclusion requires Windows.");
            return;
        }
        boolean exclude = hideFromCapture.isSelected();
        // Apply to every visible window of this process (the Swing frame).
        boolean ok = com.remotesupport.desktop.WindowAffinity.setAllCurrentProcessWindows(exclude);
        appendLog(exclude
                ? (ok ? "This window is now hidden from screen capture." : "Could not set capture exclusion.")
                : "Capture exclusion removed.");
    }

    private void sendFileToReceiver() {
        if (!receiverConnected || sessionId == null) {
            appendLog("No receiver connected yet.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        new Thread(() -> {
            try {
                long size = Files.size(file.toPath());
                ObjectNode meta = mapper.createObjectNode();
                meta.put("type", "file-message");
                meta.put("sessionId", sessionId);
                ObjectNode data = meta.putObject("data");
                data.put("kind", "download-meta");
                data.put("name", file.getName());
                data.put("size", size);
                send(meta);

                int index = 0;
                try (var in = Files.newInputStream(file.toPath())) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        ObjectNode chunk = mapper.createObjectNode();
                        chunk.put("type", "file-message");
                        chunk.put("sessionId", sessionId);
                        ObjectNode cd = chunk.putObject("data");
                        cd.put("kind", "download-chunk");
                        cd.put("name", file.getName());
                        cd.put("index", index);
                        cd.put("data", Base64.getEncoder().encodeToString(trim(buffer, read)));
                        send(chunk);
                        index++;
                        Thread.sleep(10);
                    }
                }
                ObjectNode done = mapper.createObjectNode();
                done.put("type", "file-message");
                done.put("sessionId", sessionId);
                ObjectNode dd = done.putObject("data");
                dd.put("kind", "download-end");
                dd.put("name", file.getName());
                send(done);
                appendLog("Sent file to receiver: " + file.getName());
            } catch (Exception e) {
                appendLog("File send failed: " + e.getMessage());
            }
        }, "file-send").start();
    }

    private static byte[] trim(byte[] buffer, int length) {
        if (length == buffer.length) {
            return buffer;
        }
        byte[] out = new byte[length];
        System.arraycopy(buffer, 0, out, 0, length);
        return out;
    }

    // ------------------------------------------------------------------
    // Incoming messages
    // ------------------------------------------------------------------

    private void handleMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            String type = node.path("type").asText("");
            JsonNode data = node.path("data");
            // Messages the backend relays verbatim (control events, mic audio,
            // revoke notifications) carry their fields at the top level, so
            // fall back to the node itself when there is no data wrapper.
            if (data.isMissingNode() || data.isNull()) {
                data = node;
            }
            String sessionIdFromMsg = node.path("sessionId").asText("");
            switch (type) {
                case "session-created" -> {
                    sessionId = data.path("sessionId").asText();
                    sendSessionIdUpdate();
                    appendLog("Session created. Share ID " + sessionId + " with the receiver.");
                }
                case "receiver-connected" -> {
                    receiverConnected = true;
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Status: receiver connected"));
                    appendLog("Receiver connected. Sharing screen...");
                    if (allowSharing.isSelected()) {
                        startSharing();
                    }
                }
                case "peer-disconnected" -> {
                    receiverConnected = false;
                    mouseControlGranted = false;
                    keyboardControlGranted = false;
                    AudioPlayback.stop();
                    audioFromReceiverEnabled = false;
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Status: waiting for receiver"));
                    appendLog("Receiver disconnected. Control revoked.");
                }
                case "control-request" -> handleControlRequest(data);
                case "control-response" -> {
                    boolean allowed = data.path("allowed").asBoolean();
                    appendLog(allowed ? "Receiver granted control (unexpected as sender)." : "Control denied.");
                }
                case "control-granted" -> {
                    // The receiver forwards the sender's own grant decision
                    // back so both ends agree on the live control state.
                    mouseControlGranted = data.path("mouse").asBoolean(mouseControlGranted);
                    keyboardControlGranted = data.path("keyboard").asBoolean(keyboardControlGranted);
                }
                case "control-revoke" -> {
                    mouseControlGranted = false;
                    keyboardControlGranted = false;
                    appendLog("Receiver revoked remote control.");
                }
                case "audio-message" -> {
                    if (!audioFromReceiverEnabled) {
                        AudioPlayback.start();
                        audioFromReceiverEnabled = true;
                        appendLog("Receiving live audio from receiver...");
                    }
                    AudioPlayback.play(data.path("pcm").asText("").isEmpty()
                            ? data.path("data").asText("")
                            : data.path("pcm").asText(),
                            data.path("sampleRate").asInt(24000));
                }
                case "audio-stop" -> {
                    AudioPlayback.stop();
                    audioFromReceiverEnabled = false;
                    appendLog("Receiver audio stopped.");
                }
                case "control-event" -> applyControlEvent(eventNode(data, node));
                case "file-message" -> handleFileMessage(data);
                case "get-windows" -> handleGetWindows(sessionIdFromMsg, data);
                case "control-app" -> handleControlApp(sessionIdFromMsg, data);
                case "session-error" -> appendLog("Server: " + data.path("message").asText());
                default -> {
                    /* ignore */ }
            }
        } catch (Exception e) {
            appendLog("Bad message: " + e.getMessage());
        }
    }

    private void handleControlRequest(JsonNode data) {
        String controlType = data.path("controlType").asText("mouse");
        boolean[] answer = new boolean[] { false };
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "The receiver is requesting " + controlType + " control of your computer. Allow?",
                    "Remote control request", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            answer[0] = choice == JOptionPane.YES_OPTION;
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "control-response");
            response.put("sessionId", sessionId);
            response.put("controlType", controlType);
            response.put("allowed", answer[0]);
            send(response);
            if ("mouse".equals(controlType)) {
                mouseControlGranted = answer[0];
            } else if ("keyboard".equals(controlType)) {
                keyboardControlGranted = answer[0];
            }
            appendLog(controlType + " control " + (answer[0] ? "granted" : "denied") + ".");
        });
    }

    private void applyControlEvent(JsonNode event) {
        // Never inject OS input unless the owner actually granted that
        // control type in this session.
        String kind = event.path("kind").asText("");
        boolean isKeyboard = kind.equals("keydown") || kind.equals("keyup");
        if (isKeyboard ? !keyboardControlGranted : !mouseControlGranted) {
            return;
        }
        switch (kind) {
            case "mousemove" -> InputInjector.moveMouse(
                    event.path("x").asDouble(0.5), event.path("y").asDouble(0.5));
            case "mousedown" -> {
                InputInjector.moveMouse(event.path("x").asDouble(0.5), event.path("y").asDouble(0.5));
                InputInjector.mouseButton(event.path("button").asInt(0), true);
            }
            case "mouseup" -> InputInjector.mouseButton(event.path("button").asInt(0), false);
            case "click" -> InputInjector.mouseButton(event.path("button").asInt(0), true);
            case "wheel" -> InputInjector.mouseWheel(-event.path("deltaY").asInt(0) / 100);
            case "keydown" -> {
                int vk = KeyMapper.toVirtualKey(event.path("key").asText(), event.path("code").asText());
                if (vk != 0) {
                    InputInjector.keyDown(vk);
                }
            }
            case "keyup" -> {
                int vk = KeyMapper.toVirtualKey(event.path("key").asText(), event.path("code").asText());
                if (vk != 0) {
                    InputInjector.keyUp(vk);
                }
            }
            default -> {
                /* ignore */ }
        }
    }

    private void handleFileMessage(JsonNode data) {
        String kind = data.path("kind").asText("");
        try {
            switch (kind) {
                case "list-request" -> {
                    Path dir = Path.of(data.path("path").asText(System.getProperty("user.home")));
                    ObjectNode reply = mapper.createObjectNode();
                    reply.put("type", "file-message");
                    reply.put("sessionId", sessionId);
                    ObjectNode rd = reply.putObject("data");
                    rd.put("kind", "list-result");
                    rd.put("path", dir.toString());
                    try {
                        var entries = rd.putArray("entries");
                        for (String[] entry : FileOps.listDir(dir)) {
                            ObjectNode entryNode = entries.addObject();
                            entryNode.put("name", entry[0]);
                            entryNode.put("dir", "dir".equals(entry[1]));
                            if (!"dir".equals(entry[1])) {
                                entryNode.put("size", Long.parseLong(entry[1]));
                            }
                        }
                    } catch (Exception listError) {
                        rd.put("error", listError.getMessage());
                    }
                    send(reply);
                }
                case "read-request" -> {
                    Path file = Path.of(data.path("path").asText()).normalize();
                    ObjectNode reply = mapper.createObjectNode();
                    reply.put("type", "file-message");
                    reply.put("sessionId", sessionId);
                    ObjectNode rd = reply.putObject("data");
                    rd.put("kind", "read-result");
                    rd.put("path", file.toString());
                    try {
                        rd.put("content", FileOps.readText(file, data.path("maxBytes").asInt(512 * 1024)));
                    } catch (Exception readError) {
                        rd.put("error", readError.getMessage());
                    }
                    send(reply);
                }
                case "write-request" -> {
                    Path file = Path.of(data.path("path").asText()).normalize();
                    ObjectNode reply = mapper.createObjectNode();
                    reply.put("type", "file-message");
                    reply.put("sessionId", sessionId);
                    ObjectNode rd = reply.putObject("data");
                    rd.put("kind", "write-result");
                    rd.put("path", file.toString());
                    try {
                        long written = FileOps.writeText(file, data.path("content").asText(""));
                        rd.put("bytesWritten", written);
                        appendLog("Receiver wrote " + written + " bytes to " + file);
                    } catch (Exception writeError) {
                        rd.put("error", writeError.getMessage());
                    }
                    send(reply);
                }
                case "upload-meta" -> {
                    Path target = FileOps.downloadsDir().resolve(data.path("name").asText("file")).normalize();
                    if (!FileOps.isAllowed(target)) {
                        throw new IllegalStateException("Destination outside Downloads folder");
                    }
                    Files.createDirectories(FileOps.downloadsDir());
                    Files.deleteIfExists(target);
                    downloads.put(data.path("name").asText(), target);
                    appendLog("Receiving file " + data.path("name").asText() + "...");
                }
                case "upload-chunk" -> {
                    Path target = downloads.get(data.path("name").asText());
                    if (target == null) {
                        throw new IllegalStateException("No upload meta received for this file");
                    }
                    int index = data.path("index").asInt();
                    byte[] chunk = Base64.getDecoder().decode(data.path("data").asText());
                    FileOps.appendChunk(target, index, CHUNK_SIZE, chunk);
                }
                case "upload-end" -> {
                    Path target = downloads.remove(data.path("name").asText());
                    if (target != null) {
                        appendLog("Saved file to " + target);
                    }
                }
                default -> {
                    /* receiver-side kinds are handled there */ }
            }
        } catch (Exception e) {
            appendLog("File transfer error: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Application control handlers (new)
    // ------------------------------------------------------------------

    private void handleGetWindows(String sessionIdFromMsg, JsonNode data) {
        try {
            appendLog("Fetching windows list for receiver...");
            java.util.List<ApplicationWindowManager.WindowInfo> windows =
                ApplicationWindowManager.getAllVisibleWindows();

            ObjectNode response = mapper.createObjectNode();
            response.put("type", "windows-list");
            response.put("sessionId", sessionIdFromMsg);

            var windowsArray = response.putArray("windows");
            for (ApplicationWindowManager.WindowInfo info : windows) {
                ObjectNode winNode = windowsArray.addObject();
                winNode.put("hwnd", info.hwnd);
                winNode.put("title", info.title);
                winNode.put("className", info.className);
                winNode.put("x", info.x);
                winNode.put("y", info.y);
                winNode.put("width", info.width);
                winNode.put("height", info.height);
                winNode.put("visible", info.visible);
                winNode.put("processId", info.processId);
            }

            send(response);
            appendLog("Sent " + windows.size() + " windows to receiver.");
        } catch (Exception e) {
            appendLog("Error fetching windows: " + e.getMessage());
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "windows-list");
            error.put("sessionId", sessionIdFromMsg);
            error.put("error", e.getMessage());
            send(error);
        }
    }

    private void handleControlApp(String sessionIdFromMsg, JsonNode data) {
        try {
            String action = data.path("action").asText("");

            switch (action) {
                case "click" -> {
                    double nx = data.path("normalizedX").asDouble(0.5);
                    double ny = data.path("normalizedY").asDouble(0.5);
                    ApplicationControlService.doubleClick(nx, ny);
                    appendLog("Click at (" + String.format("%.2f", nx) + ", " + String.format("%.2f", ny) + ")");
                }
                case "rightclick" -> {
                    double nx = data.path("normalizedX").asDouble(0.5);
                    double ny = data.path("normalizedY").asDouble(0.5);
                    ApplicationControlService.rightClick(nx, ny);
                    appendLog("Right-click at (" + String.format("%.2f", nx) + ", " + String.format("%.2f", ny) + ")");
                }
                case "doubleclick" -> {
                    double nx = data.path("normalizedX").asDouble(0.5);
                    double ny = data.path("normalizedY").asDouble(0.5);
                    ApplicationControlService.doubleClick(nx, ny);
                    appendLog("Double-click at (" + String.format("%.2f", nx) + ", " + String.format("%.2f", ny) + ")");
                }
                case "scroll" -> {
                    double nx = data.path("normalizedX").asDouble(0.5);
                    double ny = data.path("normalizedY").asDouble(0.5);
                    int delta = data.path("delta").asInt(0);
                    ApplicationControlService.scroll(nx, ny, delta);
                    appendLog("Scroll at (" + String.format("%.2f", nx) + ", " + String.format("%.2f", ny) + ") delta=" + delta);
                }
                case "drag" -> {
                    double fromX = data.path("fromX").asDouble(0.2);
                    double fromY = data.path("fromY").asDouble(0.2);
                    double toX = data.path("toX").asDouble(0.8);
                    double toY = data.path("toY").asDouble(0.8);
                    int duration = data.path("duration").asInt(500);
                    ApplicationControlService.dragMouse(fromX, fromY, toX, toY, duration);
                    appendLog("Drag from (" + String.format("%.2f", fromX) + ", " + String.format("%.2f", fromY) +
                             ") to (" + String.format("%.2f", toX) + ", " + String.format("%.2f", toY) + ")");
                }
                case "text" -> {
                    String text = data.path("text").asText("");
                    ApplicationControlService.sendTextViaClipboard(text);
                    appendLog("Text sent: " + text.substring(0, Math.min(50, text.length())));
                }
                case "key" -> {
                    int keyCode = data.path("keyCode").asInt(0);
                    boolean ctrl = data.path("ctrl").asBoolean(false);
                    boolean shift = data.path("shift").asBoolean(false);
                    boolean alt = data.path("alt").asBoolean(false);

                    if (ctrl) {
                        InputInjector.keyDown(0x11);  // VK_CONTROL
                        Thread.sleep(50);
                    }
                    if (shift) {
                        InputInjector.keyDown(0x10);  // VK_SHIFT
                        Thread.sleep(50);
                    }
                    if (alt) {
                        InputInjector.keyDown(0x12);  // VK_MENU (Alt)
                        Thread.sleep(50);
                    }

                    InputInjector.tapKey(keyCode);
                    Thread.sleep(50);

                    if (alt) InputInjector.keyUp(0x12);
                    if (shift) InputInjector.keyUp(0x10);
                    if (ctrl) InputInjector.keyUp(0x11);

                    appendLog("Key pressed: 0x" + Integer.toHexString(keyCode) +
                             (ctrl ? " +Ctrl" : "") + (shift ? " +Shift" : "") + (alt ? " +Alt" : ""));
                }
                case "focus" -> {
                    long hwnd = data.path("hwnd").asLong(0);
                    if (hwnd != 0) {
                        ApplicationControlService.focusApplication(hwnd);
                        appendLog("Focused window: " + hwnd);
                    }
                }
                case "minimize" -> {
                    long hwnd = data.path("hwnd").asLong(0);
                    if (hwnd != 0) {
                        ApplicationControlService.minimizeApplication(hwnd);
                        appendLog("Minimized window: " + hwnd);
                    }
                }
                case "maximize" -> {
                    long hwnd = data.path("hwnd").asLong(0);
                    if (hwnd != 0) {
                        ApplicationControlService.maximizeApplication(hwnd);
                        appendLog("Maximized window: " + hwnd);
                    }
                }
                case "close" -> {
                    long hwnd = data.path("hwnd").asLong(0);
                    if (hwnd != 0) {
                        ApplicationControlService.closeApplication(hwnd);
                        appendLog("Closed window: " + hwnd);
                    }
                }
                case "resize" -> {
                    long hwnd = data.path("hwnd").asLong(0);
                    if (hwnd != 0) {
                        int x = data.path("x").asInt(100);
                        int y = data.path("y").asInt(100);
                        int width = data.path("width").asInt(800);
                        int height = data.path("height").asInt(600);
                        ApplicationControlService.resizeApplication(hwnd, x, y, width, height);
                        appendLog("Resized window: " + hwnd);
                    }
                }
                default -> appendLog("Unknown control action: " + action);
            }

            // Send success response back
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "control-app-response");
            response.put("sessionId", sessionIdFromMsg);
            response.put("success", true);
            response.put("message", "Command executed: " + action);
            send(response);

        } catch (Exception e) {
            appendLog("Control command error: " + e.getMessage());
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "control-app-response");
            error.put("sessionId", sessionIdFromMsg);
            error.put("success", false);
            error.put("message", e.getMessage());
            send(error);
        }
    }

    /**
     * Control events arrive either nested under "event" (browser receiver,
     * raw relay) or directly in the message body. Pick whichever holds the
     * payload.
     */
    private static JsonNode eventNode(JsonNode data, JsonNode node) {
        JsonNode nested = node.path("event");
        if (!nested.isMissingNode() && !nested.isNull()) {
            return nested;
        }
        return data;
    }

    private void shutdown() {
        stopSharing();
        AudioPlayback.stop();
        mouseControlGranted = false;
        keyboardControlGranted = false;
        if (ws != null) {
            ws.close();
        }
    }

    public static void main(String[] args) {
        // Capture with Robot needs a display; non-Windows is still fine for
        // screen sharing, only SendInput/capture exclusion require Windows.
        SwingUtilities.invokeLater(() -> new DesktopSender().setVisible(true));
    }

    // ------------------------------------------------------------------
    // WebSocket client
    // ------------------------------------------------------------------

    private class WsClient extends WebSocketClient {
        WsClient(URI uri) {
            super(uri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            appendLog("Connected to backend.");
            send(mapper.createObjectNode().put("type", "create-session").toString());
        }

        @Override
        public void onMessage(String message) {
            handleMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: disconnected"));
            appendLog("Disconnected: " + reason);
            stopSharing();
        }

        @Override
        public void onError(Exception ex) {
            appendLog("WebSocket error: " + ex.getMessage());
        }
    }
}
