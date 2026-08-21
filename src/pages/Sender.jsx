import { useEffect, useRef, useState } from "react";
import "./Sender.css";

const rtcConfig = {
  iceServers: [
    {
      urls: "stun:stun.l.google.com:19302"
    }
  ]
};

const WINDOWS_CAPTURE_EXCLUSION_AFFINITY = 0x00000011;

function Sender() {
  const [sessionId, setSessionId] = useState("");
  const [connected, setConnected] = useState(false);
  const [sharing, setSharing] = useState(false);
  const [captureExclusionRequested, setCaptureExclusionRequested] = useState(false);
  const [captureExclusionStatus, setCaptureExclusionStatus] = useState("");

  const peerRef = useRef(null);
  const streamRef = useRef(null);
  const socketRef = useRef(null);

  useEffect(() => {
    const socket = new WebSocket("ws://localhost:5001/ws");
    socketRef.current = socket;

    socket.onopen = () => {
      console.log("WebSocket connected");
      socket.send(JSON.stringify({ type: "create-session" }));
    };

    socket.onmessage = async (event) => {
      const message = JSON.parse(event.data);
      const { type, data } = message;

      switch (type) {
        case "session-created":
          setSessionId(data.sessionId);
          console.log("Session ID:", data.sessionId);
          break;
        case "receiver-connected":
          console.log("Receiver connected");
          setConnected(true);
          break;
        case "answer":
          try {
            if (!peerRef.current) return;
            await peerRef.current.setRemoteDescription(
              new RTCSessionDescription(data)
            );
            console.log("Answer received");
          } catch (error) {
            console.error("Answer error:", error);
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
        case "display-affinity-result":
          setCaptureExclusionStatus(
            data.captureExcluded
              ? "Capture exclusion is active in the connected desktop client."
              : "The connected client could not enable capture exclusion."
          );
          break;
        case "session-error":
          setCaptureExclusionStatus(data.message);
          break;
        default:
          break;
      }
    };

    socket.onclose = () => {
      console.log("WebSocket closed");
    };

    socket.onerror = (error) => {
      console.error("WebSocket error", error);
    };

    return () => {
      socket.close();
    };
  }, []);

  const copySessionId = async () => {
    await navigator.clipboard.writeText(sessionId);
    alert("Session ID copied");
  };

  const updateCaptureExclusionRequest = (requested) => {
    setCaptureExclusionRequested(requested);

    if (!requested) {
      setCaptureExclusionStatus("Capture exclusion request disabled.");
      return;
    }

    if (!connected || !sessionId || socketRef.current?.readyState !== WebSocket.OPEN) {
      setCaptureExclusionStatus("Connect a receiver before requesting capture exclusion.");
      return;
    }

    socketRef.current.send(
      JSON.stringify({
        type: "display-affinity",
        sessionId,
        windowDisplayAffinity: WINDOWS_CAPTURE_EXCLUSION_AFFINITY,
        captureExclusionRequested: true,
        captureExcluded: false
      })
    );
    setCaptureExclusionStatus(
      "Request sent. A Windows desktop client must apply and confirm the setting."
    );
  };

  const startSharing = async () => {
    try {
      if (!connected) {
        alert("Please connect a receiver first.");
        return;
      }

      const stream = await navigator.mediaDevices.getDisplayMedia({
        video: true,
        audio: false
      });

      streamRef.current = stream;
      const peer = new RTCPeerConnection(rtcConfig);
      peerRef.current = peer;

      stream.getTracks().forEach((track) => {
        peer.addTrack(track, stream);
      });

      peer.onicecandidate = (event) => {
        if (event.candidate) {
          socketRef.current?.send(
            JSON.stringify({
              type: "ice-candidate",
              sessionId,
              candidate: event.candidate
            })
          );
        }
      };

      stream.getVideoTracks()[0].onended = () => {
        stopSharing();
      };

      const offer = await peer.createOffer();
      await peer.setLocalDescription(offer);

      socketRef.current?.send(
        JSON.stringify({
          type: "offer",
          sessionId,
          offer
        })
      );

      setSharing(true);
      console.log("Screen sharing started");
    } catch (error) {
      console.error("Screen share error:", error);
      alert("Screen sharing was cancelled or blocked.");
    }
  };

  const stopSharing = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }

    if (peerRef.current) {
      peerRef.current.close();
      peerRef.current = null;
    }

    setSharing(false);
    console.log("Screen sharing stopped");
  };

  return (
    <div className="sender-page">
      <header className="sender-header">
        <div className="sender-logo">
          Remote<span>Support</span>
        </div>
        <div className="sender-header-status">
          <span className={connected ? "status-dot online" : "status-dot waiting"} />
          {connected ? "Connected" : "Waiting"}
        </div>
      </header>
      <main className="sender-main">
        <div className="sender-heading">
          <h1>Share Your Computer</h1>
          <p>Allow another user to view your computer screen.</p>
        </div>
        <section className="sender-card">
          <div className="card-top">
            <div className="card-icon">🖥️</div>
            <div>
              <h2>Your Session</h2>
              <p>Share this ID with the receiver.</p>
            </div>
          </div>
          <div className="session-area">
            <label>SESSION ID</label>
            <div className="session-box">
              <strong>{sessionId || "Generating..."}</strong>
              <button onClick={copySessionId} disabled={!sessionId}>Copy</button>
            </div>
          </div>
          <div className="connection-status">
            <span className={connected ? "big-status-dot connected" : "big-status-dot"} />
            <div>
              <strong>{connected ? "Receiver connected" : "Waiting for receiver"}</strong>
              <p>{connected ? "You can now start screen sharing." : "Give the Session ID to another computer."}</p>
            </div>
          </div>
          <div className="capture-exclusion-option">
            <label>
              <input
                type="checkbox"
                checked={captureExclusionRequested}
                disabled={!connected}
                onChange={(event) => updateCaptureExclusionRequest(event.target.checked)}
              />
              Request capture exclusion for this application window
            </label>
            <p>
              This requires a Windows desktop client and never affects other applications.
            </p>
            {captureExclusionStatus && (
              <p className="capture-exclusion-status" role="status">
                {captureExclusionStatus}
              </p>
            )}
          </div>
          {!sharing ? (
            <button className="share-button" onClick={startSharing} disabled={!connected}>
              🖥️ Start Screen Sharing
            </button>
          ) : (
            <button className="stop-button" onClick={stopSharing}>
              ■ Stop Screen Sharing
            </button>
          )}
        </section>
        <section className="control-card">
          <div className="control-heading">
            <div className="control-icon">🖱️</div>
            <div>
              <h2>Remote Control</h2>
              <p>Control access is permission based.</p>
            </div>
          </div>
          <div className="control-options">
            <div className="control-item">
              <span>🖱️</span>
              <div>
                <strong>Mouse Control</strong>
                <small>Permission required</small>
              </div>
              <span className="locked">🔒</span>
            </div>
            <div className="control-item">
              <span>⌨️</span>
              <div>
                <strong>Keyboard Control</strong>
                <small>Permission required</small>
              </div>
              <span className="locked">🔒</span>
            </div>
          </div>
          <div className="permission-message">🔐 Remote control requires your approval.</div>
        </section>
      </main>
    </div>
  );
}

export default Sender;
