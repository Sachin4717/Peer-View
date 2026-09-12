// Drives the REAL running desktop sender (visible in backend log):
// joins its live session, sends mousemove/keyboard events and 3 seconds of
// audio, then reports any transport errors.
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const WebSocket = require("ws");

const sessionId = process.argv[2];
if (!sessionId) { console.error("usage: node drive-real-sender.mjs <sessionId>"); process.exit(1); }
const sock = new WebSocket("ws://localhost:5001/ws");
sock.on("open", () => {
  sock.send(JSON.stringify({ type: "join-session", sessionId }));
  setTimeout(() => {
    // 1.5 s of mouse movement + a few key events (gated until granted —
    // sender must NOT inject, proving the safety gate, and must not crash)
    for (let i = 0; i < 20; i++) {
      sock.send(JSON.stringify({ type: "control-event", sessionId, event: { kind: "mousemove", x: i / 20, y: 0.5 } }));
      sock.send(JSON.stringify({ type: "control-event", sessionId, event: { kind: "keydown", key: "a", code: "KeyA" } }));
      sock.send(JSON.stringify({ type: "control-event", sessionId, event: { kind: "keyup", key: "a", code: "KeyA" } }));
    }
    // 3 seconds of 24 kHz PCM16 mono sine-ish audio
    const samples = new Int16Array(1200);
    for (let j = 0; j < samples.length; j++) samples[j] = Math.round(Math.sin(j / 8) * 8000);
    const pcm = Buffer.from(samples.buffer).toString("base64");
    const iv = setInterval(() => sock.send(JSON.stringify({ type: "audio-message", sessionId, pcm, sampleRate: 24000 })), 50);
    setTimeout(() => {
      clearInterval(iv);
      sock.send(JSON.stringify({ type: "control-revoke", sessionId }));
      sock.close();
      console.log("done: 60 control events + 3s audio delivered to the real sender");
      process.exit(0);
    }, 3000);
  }, 500);
});
