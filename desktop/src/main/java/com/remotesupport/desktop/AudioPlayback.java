package com.remotesupport.desktop;

import java.io.ByteArrayOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays real-time PCM16 mono audio chunks streamed from the receiver over the
 * WebSocket (the remote microphone), so the sender hears the receiver the way
 * AnyDesk does. Chunks arrive roughly every 20-40 ms; the line simply absorbs
 * them. If the peer restarts with a different sample rate the line is rebuilt.
 */
public final class AudioPlayback {

    private static final Object LOCK = new Object();
    private static SourceDataLine line;
    private static int lineSampleRate = -1;
    private static boolean running;

    private AudioPlayback() {
    }

    /** Feeds one base64 PCM16 little-endian mono chunk to the speaker. */
    public static void play(String base64Pcm, int sampleRate) {
        if (base64Pcm == null || base64Pcm.isBlank() || sampleRate <= 0) {
            return;
        }
        byte[] pcm;
        try {
            pcm = java.util.Base64.getDecoder().decode(base64Pcm);
        } catch (IllegalArgumentException e) {
            return;
        }
        synchronized (LOCK) {
            if (!running) {
                return;
            }
            try {
                ensureLine(sampleRate);
                if (line != null) {
                    line.write(pcm, 0, pcm.length);
                }
            } catch (LineUnavailableException | IllegalStateException e) {
                close();
            }
        }
    }

    public static void start() {
        synchronized (LOCK) {
            running = true;
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            running = false;
            close();
        }
    }

    public static boolean isRunning() {
        synchronized (LOCK) {
            return running;
        }
    }

    private static void ensureLine(int sampleRate) throws LineUnavailableException {
        if (line != null && lineSampleRate == sampleRate) {
            return;
        }
        if (line != null) {
            line.close();
            line = null;
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        line = (SourceDataLine) AudioSystem.getLine(
                new DataLine.Info(SourceDataLine.class, format));
        // ~50 ms internal buffer: low latency, but big enough to absorb
        // jitter between the ~40 ms WebSocket chunks without underruns.
        line.open(format, sampleRate / 20 * 2);
        line.start();
        lineSampleRate = sampleRate;
    }

    private static void close() {
        if (line != null) {
            try {
                line.flush();
                line.close();
            } catch (IllegalStateException ignored) {
                // already closed
            }
            line = null;
            lineSampleRate = -1;
        }
    }

    /** Utility kept for potential resampling extensions. */
    static byte[] concat(byte[] a, byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(a.length + b.length);
        out.writeBytes(a);
        out.writeBytes(b);
        return out.toByteArray();
    }
}
