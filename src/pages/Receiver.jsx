import { useEffect, useRef, useState } from "react";
import "./Receiver.css";

const socketUrl = "ws://localhost:5001/ws";

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

  const videoRef = useRef(null);
  const peerRef = useRef(null);
  const socketRef = useRef(null);
  const sessionRef = useRef("");

  useEffect(() => {
    const socket = new WebSocket(socketUrl);
    socketRef.current = socket;

    socket.onopen = () => {
      console.log("WebSocket connected");
    };

    socket.onmessage = async (event) => {
      const message = JSON.parse(event.data);
      const { type, data } = message;

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
    };

    socket.onerror = (error) => {
      console.error("WebSocket error", error);
    };

    return () => {
      socket.close();
    };
  }, []);

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

  const disconnect = () => {

    if (peerRef.current) {

      peerRef.current.close();

      peerRef.current = null;

    }


    if (videoRef.current) {

      videoRef.current.srcObject = null;

    }


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

              <video
                ref={videoRef}
                autoPlay
                playsInline
              />

              <div className="video-status">
                🟢 Live
              </div>

            </div>

          </section>

        )}

      </main>

    </div>

  );
}

export default Receiver;
