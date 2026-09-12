const ws = new WebSocket('ws://localhost:5001/ws');
ws.onopen = () => { console.log('WS OPEN'); ws.close(); };
ws.onerror = (e) => console.log('WS ERROR');
ws.onclose = (e) => console.log('WS CLOSED code=' + e.code);
setTimeout(() => process.exit(0), 5000);
