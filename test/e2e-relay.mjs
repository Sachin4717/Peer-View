// End-to-end relay test: runs against the REAL backend on ws://localhost:5001/ws
// Simulates receiver (browser) and sender (desktop client) and asserts that
// control requests, grants, control events and real-time audio chunks all
// traverse the backend correctly.
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const WebSocket = require("ws");

const URL = "ws://localhost:5001/ws";
let pass = 0, fail = 0;
const ok = (name, cond) => {
  if (cond) { pass++; console.log(`  PASS ${name}`); }
  else { fail++; console.log(`  FAIL ${name}`); }
};

const sender = new WebSocket(URL);
const receiver = new WebSocket(URL);
const queued = new Map(); // socket -> messages not yet consumed

function waitFor(sock, type, timeoutMs = 3000) {
  return new Promise((resolve, reject) => {
    const q = queued.get(sock) || [];
    const idx = q.findIndex((m) => m.type === type);
    if (idx >= 0) { queued.set(sock, q.filter((_, i) => i !== idx)); return resolve(q[idx]); }
    const timer = setTimeout(() => reject(new Error(`timeout waiting for ${type}`)), timeoutMs);
    const entry = { type, fn: (msg) => { if (msg.type === type) { clearTimeout(timer); resolve(msg); } else { q.push(msg); } } };
    (sock.__waiters ||= []).push(entry);
  });
}
function wire(sock) {
  sock.on("message", (d) => {
    const msg = JSON.parse(d.toString());
    (sock.__waiters ||= []).forEach((w) => w.fn(msg));
    sock.__waiters = (sock.__waiters || []).filter((w) => w.type !== msg.type);
  });
}

let sessionId;
const pcmChunk = Buffer.from(new Int16Array(2048).fill(500)).toString("base64");

await new Promise((res) => {
  let open = 0;
  sender.on("open", () => { if (++open === 2) res(); });
  receiver.on("open", () => { if (++open === 2) res(); });
});
wire(sender); wire(receiver);
sender.__audioCount = 0;
sender.on("message", (d) => {
  const m = JSON.parse(d.toString());
  if (m.type === "audio-message") sender.__audioCount++;
});

console.log("\n== 1. Session create + join ==");
sender.send(JSON.stringify({ type: "create-session" }));
const created = await waitFor(sender, "session-created");
sessionId = created.data.sessionId;
ok("sender receives 8-digit session id", /^\d{8}$/.test(sessionId));

receiver.send(JSON.stringify({ type: "join-session", sessionId }));
const joined = await waitFor(receiver, "session-joined");
ok("receiver joins", joined.data.sessionId === sessionId);
const connected = await waitFor(sender, "receiver-connected");
ok("sender notified of receiver", !!connected);

console.log("\n== 2. Control request + grant (AnyDesk-style) ==");
receiver.send(JSON.stringify({ type: "control-request", sessionId, controlType: "mouse" }));
const ctrlReq = await waitFor(sender, "control-request");
ok("sender gets control-request with controlType", ctrlReq.controlType === "mouse");

sender.send(JSON.stringify({ type: "control-response", sessionId, controlType: "mouse", allowed: true }));
const ctrlRes = await waitFor(receiver, "control-response");
ok("receiver gets grant allowed=true", ctrlRes.controlType === "mouse" && ctrlRes.allowed === true);

console.log("\n== 3. Control events (mouse move + keyboard) relay to sender ==");
receiver.send(JSON.stringify({ type: "control-event", sessionId, event: { kind: "mousemove", x: 0.42, y: 0.17 } }));
const ev1 = await waitFor(sender, "control-event");
ok("mousemove event relayed", ev1.event?.kind === "mousemove" && ev1.event.x === 0.42);

receiver.send(JSON.stringify({ type: "control-event", sessionId, event: { kind: "keydown", key: "a", code: "KeyA", ctrlKey: false } }));
const ev2 = await waitFor(sender, "control-event");
ok("keydown KeyA relayed", ev2.event?.kind === "keydown" && ev2.event.code === "KeyA");

console.log("\n== 4. Real-time receiver -> sender audio ==");
for (let i = 0; i < 5; i++) {
  receiver.send(JSON.stringify({ type: "audio-message", sessionId, pcm: pcmChunk, sampleRate: 24000 }));
}
const audio = await waitFor(sender, "audio-message");
ok("audio chunk reaches sender", audio.pcm === pcmChunk && audio.sampleRate === 24000);
await new Promise((r) => setTimeout(r, 500));
const audioTotal = sender.__audioCount || 0;
ok("all 5 audio chunks delivered in 500ms", audioTotal === 5);

console.log("\n== 5. Revoke + disconnect safety ==");
receiver.send(JSON.stringify({ type: "control-revoke", sessionId }));
const revoked = await waitFor(sender, "control-revoke");
ok("sender receives control-revoke", !!revoked);

receiver.close();
const peerGone = await waitFor(sender, "peer-disconnected");
ok("sender notified when receiver drops", peerGone.data.role === "receiver");

sender.close();
console.log(`\nRESULT: ${pass} passed, ${fail} failed`);
process.exit(fail ? 1 : 0);
