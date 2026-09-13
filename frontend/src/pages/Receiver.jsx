import { useEffect, useRef, useState } from "react";
import "./Receiver.css";

const socketUrl = import.meta.env.VITE_WS_URL || "ws://localhost:5001/ws";

const rtcConfig = {
  iceServers: [
    {
      urls: "stun:stun.l.google.com:19302"
    }
  ]
};

function Receiver() {
  const [sessionId, setSessionId] = useState("");
  const [connecting, setConnecting] = useState(false);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState("");
  const [captureExclusionStatus, setCaptureExclusionStatus] = useState("");
  const [voiceActive, setVoiceActive] = useState(false);
  const [voiceStatus, setVoiceStatus] = useState("");
  const [controlGranted, setControlGranted] = useState({ mouse: false, keyboard: false });
  const [controlStatus, setControlStatus] = useState("");
  const [desktopMode, setDesktopMode] = useState(false);
  const [fileStatus, setFileStatus] = useState("");
  const [explorerPath, setExplorerPath] = useState("");
  const [explorerEntries, setExplorerEntries] = useState(null);
  const [editor, setEditor] = useState(null); // { path, content }

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const peerRef = useRef(null);
  const socketRef = useRef(null);
  const sessionRef = useRef("");
  const controlChannelRef = useRef(null);
  const controlGrantedRef = useRef({ mouse: false, keyboard: false });
  const chunksRef = useRef(null);

  useEffect(() => {
    const socket = new WebSocket(socketUrl);
    socketRef.current = socket;

    socket.onopen = () => {
      console.log("WebSocket connected");
    };

    socket.onmessage = async (event) => {
      const message = JSON.parse(event.data);
      const { type } = message;
      // Relayed messages (control, audio, revoke) carry fields at the top
      // level; server-generated notifications wrap them in `data`.
      const data = message.data ?? message;

      switch (type) {
        case "session-joined":
          sessionRef.current = data.sessionId;
          console.log("Session joined:", data.sessionId);
          setConnecting(false);
          setConnected(true);
          setError("");
          break;
        case "offer":
          try {
            console.log("Offer received");
            const peer = new RTCPeerConnection(rtcConfig);
            peerRef.current = peer;

            peer.ontrack = (event) => {
              console.log("Screen track received");
              if (videoRef.current) {
                videoRef.current.srcObject = event.streams[0];
              }
            };

            peer.ondatachannel = (channelEvent) => {
              if (channelEvent.channel.label === "control") {
                controlChannelRef.current = channelEvent.channel;
              }
            };

            peer.onicecandidate = (event) => {
              if (event.candidate) {
                socket.send(
                  JSON.stringify({
                    type: "ice-candidate",
                    sessionId: sessionRef.current,
                    candidate: event.candidate
                  })
                );
              }
            };

            await peer.setRemoteDescription(new RTCSessionDescription(data));
            const answer = await peer.createAnswer();
            await peer.setLocalDescription(answer);

            socket.send(
              JSON.stringify({
                type: "answer",
                sessionId: sessionRef.current,
                answer
              })
            );

            console.log("Answer sent");
          } catch (error) {
            console.error("WebRTC error:", error);
            setError("Unable to establish screen connection.");
          }
          break;
        case "ice-candidate":
          try {
            if (peerRef.current && data) {
              await peerRef.current.addIceCandidate(new RTCIceCandidate(data));
            }
          } catch (error) {
            console.error("ICE error:", error);
          }
          break;
        case "control-response":
          {
            const controlType = data?.controlType;
            const allowed = Boolean(data?.allowed);
            if (controlType === "mouse" || controlType === "keyboard") {
              controlGrantedRef.current = {
                ...controlGrantedRef.current,
                [controlType]: allowed
              };
              setControlGranted(controlGrantedRef.current);
              setControlStatus(
                allowed
                  ? `${controlType === "mouse" ? "Mouse" : "Keyboard"} control granted. Click the video and start controlling.`
                  : `Sender denied ${controlType} control.`
              );
            }
          }
          break;
        case "peer-disconnected":
          // The other side dropped: kill live audio + control state.
          stopMicShare();
          controlGrantedRef.current = { mouse: false, keyboard: false };
          setControlGranted({ mouse: false, keyboard: false });
          setControlStatus("The peer disconnected. Control was revoked.");
          break;
        case "screen-frame":
          // A native desktop sender streams raw screen frames; draw them on a
          // canvas instead of a WebRTC video element.
          if (!desktopMode) {
            setDesktopMode(true);
          }
          drawScreenFrame(data.data);
          break;
        case "file-message":
          handleFileMessage(data);
          break;
        case "session-error":
          setConnecting(false);
          setConnected(false);
          setError(data.message);
          break;
        case "display-affinity":
          setCaptureExclusionStatus(
            "This browser cannot apply Windows capture exclusion. A desktop client is required."
          );
          if (socket.readyState === WebSocket.OPEN && sessionRef.current) {
            socket.send(
              JSON.stringify({
                type: "display-affinity-result",
                sessionId: sessionRef.current,
                windowDisplayAffinity: data.windowDisplayAffinity,
                captureExclusionRequested: Boolean(data.captureExclusionRequested),
                captureExcluded: false
              })
            );
          }
          break;
        default:
          break;
      }
    };

    socket.onclose = () => {
      console.log("WebSocket closed");
      // Stop the microphone chain so no timer keeps sending on the dead
      // socket (this was flooding the console with send errors).
      stopMicShare();
      setConnected(false);
      setConnecting(false);
    };

    socket.onerror = (error) => {
      console.error("WebSocket error", error);
    };

    return () => {
      socket.close();
    };
  }, []);

  // ---------------------------------------------------------------
  // Live microphone -> sender (AnyDesk-style voice, real time)
  // (pass)
  // ---------------------------------------------------------------

  const micStreamRef = useRef(null);
  const audioContextRef = useRef(null);
  const micActiveRef = useRef(false);

  const startMicShare = async () => {
    if (micActiveRef.current) return;
    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) {
      setVoiceStatus("Connection is not open.");
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true }
      });
      micStreamRef.current = stream;
      // IMPORTANT: create the AudioContext at its NATIVE sample rate. Forcing
      // e.g. 24000 makes Chrome compute a 1200-frame default ScriptProcessor
      // buffer (device rate / 40) and createScriptProcessor then throws
      // IndexSizeError. The sender rebuilds its playback line for whatever
      // rate we report, so the native rate (usually 44100/48000) is best.
      const context = new AudioContext();
      const SAMPLE_RATE = context.sampleRate;
      audioContextRef.current = context;
      const source = context.createMediaStreamSource(stream);
      const processor = context.createScriptProcessor(2048, 1, 1); // ~40-45 ms
      processor.onaudioprocess = (e) => {
        if (!micActiveRef.current) return;
        const input = e.inputBuffer.getChannelData(0);
        // Convert float32 -> int16 little-endian PCM.
        const pcm = new Int16Array(input.length);
        for (let i = 0; i < input.length; i++) {
          const s = Math.max(-1, Math.min(1, input[i]));
          pcm[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
        }
        const bytes = new Uint8Array(pcm.buffer);
        let binary = "";
        for (let i = 0; i < bytes.length; i++) {
          binary += String.fromCharCode(bytes[i]);
        }
        // Only send while the socket is actually usable; sending on a closed
        // socket throws "already in CLOSING or CLOSED state" every 85 ms.
        const socket = socketRef.current;
        if (!socket || socket.readyState !== WebSocket.OPEN) {
          stopMicShare();
          setVoiceStatus("Connection lost — microphone sharing stopped.");
          return;
        }
        socket.send(JSON.stringify({
          type: "audio-message",
          sessionId: sessionRef.current,
          pcm: btoa(binary),
          sampleRate: SAMPLE_RATE
        }));
        // (SAMPLE_RATE is read from the closure above; it is the context's
        // real rate, so the sender opens a matching speaker line.)
      };
      source.connect(processor);
      // ScriptProcessors only run when connected to the destination, but the
      // mic must NOT reach the local speakers (feedback echo). A zero-gain
      // node keeps the graph alive while silencing the local output.
      const silentSink = context.createGain();
      silentSink.gain.value = 0;
      processor.connect(silentSink);
      silentSink.connect(context.destination);
      micActiveRef.current = true;
      setVoiceActive(true);
      setVoiceStatus("🎤 Your microphone is live — the sender can hear you.");
    } catch (err) {
      console.error("Mic share failed:", err);
      setVoiceStatus("Microphone access denied.");
    }
  };

  const stopMicShare = () => {
    micActiveRef.current = false;
    micStreamRef.current?.getTracks().forEach((t) => t.stop());
    micStreamRef.current = null;
    audioContextRef.current?.close().catch(() => {});
    audioContextRef.current = null;
    setVoiceActive(false);
    if (socketRef.current?.readyState === WebSocket.OPEN && sessionRef.current) {
      socketRef.current.send(JSON.stringify({
        type: "audio-stop",
        sessionId: sessionRef.current
      }));
    }
  };

  const revokeControl = () => {
    if (socketRef.current?.readyState === WebSocket.OPEN && sessionRef.current) {
      socketRef.current.send(JSON.stringify({
        type: "control-revoke",
        sessionId: sessionRef.current
      }));
    }
    controlGrantedRef.current = { mouse: false, keyboard: false };
    setControlGranted({ mouse: false, keyboard: false });
    setControlStatus("Remote control revoked.");
  };

  const connectToComputer = () => {
    const id = sessionId.trim();
    setError("");

    if (!id) {
      setError("Please enter Session ID.");
      return;
    }

    if (!/^\d{8}$/.test(id)) {
      setError("Session ID must contain 8 digits.");
      return;
    }

    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) {
      setError("WebSocket connection is not open.");
      return;
    }

    setConnecting(true);
    socketRef.current.send(
      JSON.stringify({
        type: "join-session",
        sessionId: id
      })
    );
  };
  // =================================

  // ---------------------------------------------------------------------
  // Remote control (mouse / keyboard)
  // ---------------------------------------------------------------------

  const requestControl = (controlType) => {
    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) {
      setControlStatus("Connection is not open.");
      return;
    }
    setControlStatus(`Requesting ${controlType} control from the sender...`);
    socketRef.current.send(
      JSON.stringify({
        type: "control-request",
        sessionId: sessionRef.current,
        controlType
      })
    );
  };

  const sendControlEvent = (event) => {
    if (desktopMode) {
      // The native desktop sender does not use a WebRTC data channel; relay
      // the event through the backend to the sender's WebSocket.
      const socket = socketRef.current;
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(
          JSON.stringify({
            type: "control-event",
            sessionId: sessionRef.current,
            event
          })
        );
      }
      return;
    }
    const channel = controlChannelRef.current;
    if (channel && channel.readyState === "open") {
      channel.send(JSON.stringify({ type: "control-event", ...event }));
    }
  };

  // ---------------------------------------------------------------
  // Desktop sender mode: canvas rendering + file transfer
  // ---------------------------------------------------------------

  const drawScreenFrame = (base64) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const image = new Image();
    image.onload = () => {
      canvas.width = image.width;
      canvas.height = image.height;
      canvas.getContext("2d").drawImage(image, 0, 0);
    };
    image.src = `data:image/jpeg;base64,${base64}`;
  };

  const sendFileMessage = (data) => {
    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: "file-message", sessionId: sessionRef.current, data }));
    }
  };

  const uploadFileToSender = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;
      const CHUNK = 256 * 1024;
      const total = Math.ceil(file.size / CHUNK);
      setFileStatus(`Uploading ${file.name} (0/${total})...`);
      sendFileMessage({ kind: "upload-meta", name: file.name, size: file.size });
      for (let index = 0; index < total; index++) {
        const slice = file.slice(index * CHUNK, (index + 1) * CHUNK);
        const buffer = await slice.arrayBuffer();
        const base64 = btoa(
          Array.from(new Uint8Array(buffer), (b) => String.fromCharCode(b)).join("")
        );
        sendFileMessage({ kind: "upload-chunk", name: file.name, index, data: base64 });
        if (index % 8 === 0) {
          setFileStatus(`Uploading ${file.name} (${index + 1}/${total})...`);
          await new Promise((resolve) => setTimeout(resolve, 15));
        }
      }
      sendFileMessage({ kind: "upload-end", name: file.name });
      setFileStatus(`Uploaded ${file.name} to the sender's Downloads folder.`);
    };
    input.click();
  };

  const listSenderDir = (path) => {
    setExplorerEntries(null);
    sendFileMessage({ kind: "list-request", path });
  };

  const openSenderFile = (path) => {
    sendFileMessage({ kind: "read-request", path, maxBytes: 512 * 1024 });
  };

  const saveSenderFile = () => {
    if (!editor) return;
    sendFileMessage({ kind: "write-request", path: editor.path, content: editor.content });
  };

  const downloadFileFromSender = () => {
    const name = window.prompt("File name in the sender's Downloads folder:");
    if (!name) return;
    chunksRef.current = {};
    setFileStatus(`Requesting ${name} from the sender...`);
    sendFileMessage({ kind: "download-request", name });
  };

  const handleFileMessage = (data) => {
    const kind = data?.kind;
    if (kind === "list-result") {
      setExplorerPath(data.path);
      if (data.error) {
        setFileStatus(`Cannot open ${data.path}: ${data.error}`);
        setExplorerEntries(null);
      } else {
        setExplorerEntries(data.entries || []);
        setFileStatus(`Loaded ${data.path}`);
      }
    } else if (kind === "read-result") {
      if (data.error) {
        setFileStatus(`Cannot read ${data.path}: ${data.error}`);
      } else {
        setEditor({ path: data.path, content: data.content });
        setFileStatus(`Opened ${data.path} for reading/writing.`);
      }
    } else if (kind === "write-result") {
      if (data.error) {
        setFileStatus(`Cannot write ${data.path}: ${data.error}`);
      } else {
        setFileStatus(`Saved ${data.bytesWritten} bytes to ${data.path} on the sender.`);
      }
    } else if (kind === "download-meta") {
      chunksRef.current = { name: data.name, size: data.size, parts: {} };
      setFileStatus(`Downloading ${data.name} (${Math.round(data.size / 1024)} KB)...`);
    } else if (kind === "download-chunk") {
      const state = chunksRef.current;
      if (state && state.name === data.name) {
        state.parts[data.index] = data.data;
      }
    } else if (kind === "download-end") {
      const state = chunksRef.current;
      if (state && state.name === data.name) {
        const indexes = Object.keys(state.parts).map(Number).sort((a, b) => a - b);
        const binary = indexes.flatMap((index) =>
          Array.from(atob(state.parts[index]), (ch) => ch.charCodeAt(0))
        );
        const blob = new Blob([new Uint8Array(binary)]);
        const link = document.createElement("a");
        link.href = URL.createObjectURL(blob);
        link.download = data.name;
        link.click();
        URL.revokeObjectURL(link.href);
        setFileStatus(`Saved ${data.name} (${binary.length} bytes).`);
        chunksRef.current = null;
      }
    }
  };

  const handleMouseActivity = (event) => {
    const video = desktopMode ? canvasRef.current : videoRef.current;
    if (!video || !controlGrantedRef.current.mouse) return;

    const rect = video.getBoundingClientRect();
    const base = {
      x: (event.clientX - rect.left) / rect.width,
      y: (event.clientY - rect.top) / rect.height
    };

    if (event.type === "mousemove") {
      sendControlEvent({ kind: "mousemove", ...base });
    } else if (event.type === "mousedown" || event.type === "mouseup") {
      sendControlEvent({ kind: event.type, ...base, button: event.button });
    } else if (event.type === "wheel") {
      sendControlEvent({
        kind: "wheel",
        ...base,
        deltaY: event.deltaY
      });
    }
  };

  // Forward keyboard input while keyboard control has been granted.
  useEffect(() => {
    if (!controlGranted.keyboard) {
      return undefined;
    }

    const handleKeyDown = (event) => {
      sendControlEvent({
        kind: "keydown",
        key: event.key,
        code: event.code,
        ctrlKey: event.ctrlKey,
        altKey: event.altKey,
        shiftKey: event.shiftKey
      });
    };

    const handleKeyUp = (event) => {
      sendControlEvent({
        kind: "keyup",
        key: event.key,
        code: event.code,
        ctrlKey: event.ctrlKey,
        altKey: event.altKey,
        shiftKey: event.shiftKey
      });
    };

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
    };
  }, [controlGranted.keyboard]);

  const disconnect = () => {

    stopMicShare();

    revokeControl();

    if (peerRef.current) {

      peerRef.current.close();

      peerRef.current = null;

    }

    controlChannelRef.current = null;
    controlGrantedRef.current = { mouse: false, keyboard: false };
    setControlGranted({ mouse: false, keyboard: false });
    setControlStatus("");
    setDesktopMode(false);
    setFileStatus("");
    chunksRef.current = null;
    setExplorerEntries(null);
    setEditor(null);


    if (videoRef.current) {

      videoRef.current.srcObject = null;

    }


    setVoiceActive(false);
    setConnected(false);
    setConnecting(false);
    setError("");

  };


  return (

    <div className="receiver-page">

      <header className="receiver-header">

        <div className="receiver-logo">
          Remote<span>Support</span>
        </div>


        <div className="receiver-status">

          <span
            className={
              connected
                ? "receiver-dot connected"
                : connecting
                ? "receiver-dot connecting"
                : "receiver-dot"
            }
          />

          {connected
            ? "Connected"
            : connecting
            ? "Connecting..."
            : "Ready"}

        </div>

      </header>


      <main className="receiver-main">

        <div className="receiver-heading">

          <h1>
            Connect to a Computer
          </h1>

          <p>
            Enter the Session ID provided
            by the computer owner.
          </p>

        </div>


        {!connected ? (

          <section className="receiver-card">

            <div className="receiver-card-icon">
              🔗
            </div>


            <h2>
              Connect to Session
            </h2>


            <p className="receiver-description">
              Enter the unique Session ID
              to request a connection.
            </p>


            <div className="input-group">

              <label>
                SESSION ID
              </label>


              <input
                type="text"
                value={sessionId}
                onChange={(e) => {

                  const value =
                    e.target.value
                      .replace(/\D/g, "")
                      .slice(0, 8);

                  setSessionId(value);
                  setError("");

                }}
                placeholder="Example: 73518429"
                disabled={connecting}
              />

            </div>


            {error && (

              <div className="receiver-error">
                ⚠️ {error}
              </div>

            )}


            {!connecting ? (

              <button
                className="connect-button"
                onClick={connectToComputer}
              >
                Connect
              </button>

            ) : (

              <button
                className="cancel-button"
                onClick={() => {
                  setConnecting(false);
                }}
              >
                Cancel Connection
              </button>

            )}


            <div className="permission-box">

              <div className="permission-icon">
                🔐
              </div>


              <div>

                <strong>
                  Permission Required
                </strong>


                <p>
                  The computer owner must approve
                  screen sharing before it begins.
                </p>

              </div>

            </div>

          </section>

        ) : (

          <section className="screen-card">

            <div className="screen-header">

              <div>

                <h2>
                  Remote Screen
                </h2>

                <p>
                  Live screen sharing
                </p>

                {captureExclusionStatus && (
                  <p role="status">{captureExclusionStatus}</p>
                )}

              </div>


              <button
                className="disconnect-button"
                onClick={disconnect}
              >
                Disconnect
              </button>

            </div>


            <div className="video-container">

              {desktopMode ? (
                <canvas
                  ref={canvasRef}
                  className={controlGranted.mouse ? "remote-controllable" : undefined}
                  onMouseMove={handleMouseActivity}
                  onMouseDown={handleMouseActivity}
                  onMouseUp={handleMouseActivity}
                  onWheel={handleMouseActivity}
                />
              ) : (
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  className={controlGranted.mouse ? "remote-controllable" : undefined}
                  onMouseMove={handleMouseActivity}
                  onMouseDown={handleMouseActivity}
                  onMouseUp={handleMouseActivity}
                  onWheel={handleMouseActivity}
                />
              )}

              <div className="video-status">
                🟢 Live
              </div>

            </div>

            {desktopMode && (
              <div className="control-toolbar" style={{ marginTop: 10 }}>
                <button className="control-request-button" onClick={uploadFileToSender}>
                  📤 Upload file to sender
                </button>
                <button className="control-request-button" onClick={downloadFileFromSender}>
                  📥 Download file from sender
                </button>
                <button
                  className="control-request-button"
                  onClick={() => listSenderDir(explorerPath || "")}
                >
                  📁 File explorer
                </button>
              </div>
            )}

            {desktopMode && explorerEntries && (
              <div className="file-explorer" style={{ marginTop: 10, textAlign: "left" }}>
                <div style={{ display: "flex", gap: 6, marginBottom: 8 }}>
                  <input
                    type="text"
                    value={explorerPath}
                    onChange={(e) => setExplorerPath(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") listSenderDir(explorerPath);
                    }}
                    placeholder="Path on the sender, e.g. C:\Users"
                    style={{ flex: 1 }}
                  />
                  <button className="control-request-button" onClick={() => listSenderDir(explorerPath)}>
                    Open
                  </button>
                </div>
                <ul style={{ margin: 0, paddingLeft: 16, maxHeight: 220, overflowY: "auto" }}>
                  {explorerEntries.map((entry) => (
                    <li key={entry.name} style={{ margin: "2px 0" }}>
                      <a
                        href="#open"
                        onClick={(e) => {
                          e.preventDefault();
                          const sep = explorerPath.includes("\\") ? "\\" : "/";
                          const child = explorerPath.replace(/[\\/]$/, "") + sep + entry.name;
                          if (entry.dir) {
                            listSenderDir(child);
                          } else {
                            openSenderFile(child);
                          }
                        }}
                      >
                        {entry.dir ? "📁" : "📄"} {entry.name}
                        {!entry.dir && entry.size != null && (
                          <span style={{ opacity: 0.6 }}> ({Math.max(1, Math.round(entry.size / 1024))} KB)</span>
                        )}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {desktopMode && editor && (
              <div className="file-editor" style={{ marginTop: 10, textAlign: "left" }}>
                <div style={{ display: "flex", gap: 6, marginBottom: 6, alignItems: "center" }}>
                  <strong style={{ flex: 1, wordBreak: "break-all" }}>✏️ {editor.path}</strong>
                  <button className="control-request-button granted" onClick={saveSenderFile}>
                    💾 Save on sender
                  </button>
                  <button className="control-request-button" onClick={() => setEditor(null)}>
                    Close
                  </button>
                </div>
                <textarea
                  value={editor.content}
                  onChange={(e) => setEditor({ ...editor, content: e.target.value })}
                  style={{ width: "100%", minHeight: 180, fontFamily: "monospace", fontSize: 12 }}
                />
              </div>
            )}

            {desktopMode && fileStatus && (
              <div className="control-status" role="status">
                {fileStatus}
              </div>
            )}

            {controlGranted.keyboard && (
              <p className="keyboard-hint">
                Keyboard is active — press keys anywhere on this page.
              </p>
            )}

            <div className="control-toolbar">

              <button
                className={
                  controlGranted.mouse
                    ? "control-request-button granted"
                    : "control-request-button"
                }
                onClick={() => requestControl("mouse")}
              >
                🖱️ {controlGranted.mouse ? "Mouse control active" : "Request mouse control"}
              </button>

              <button
                className={
                  controlGranted.keyboard
                    ? "control-request-button granted"
                    : "control-request-button"
                }
                onClick={() => requestControl("keyboard")}
              >
                ⌨️ {controlGranted.keyboard ? "Keyboard control active" : "Request keyboard control"}
              </button>

            </div>

            {controlStatus && (
              <div className="control-status" role="status">
                {controlStatus}
              </div>
            )}

            <div className={
              voiceActive
                ? "voice-status active"
                : "voice-status"
            }>
              {voiceActive
                ? "🎧 Voice sharing active"
                : "🎧 Share your microphone with the sender"}
            </div>

            <div className="control-toolbar">
              <button
                className={voiceActive ? "control-request-button granted" : "control-request-button"}
                onClick={() => (voiceActive ? stopMicShare() : startMicShare())}
              >
                {voiceActive ? "🔇 Stop microphone" : "🎤 Share my microphone (sender hears me)"}
              </button>
              <button className="control-request-button" onClick={revokeControl}>
                🚫 Revoke remote control
              </button>
            </div>
            {voiceStatus && (
              <div className="control-status" role="status">{voiceStatus}</div>
            )}

          </section>

        )}

      </main>

    </div>

  );
}

export default Receiver;
