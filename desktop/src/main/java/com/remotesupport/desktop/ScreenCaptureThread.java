package com.remotesupport.desktop;

import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * Captures the whole virtual screen with java.awt.Robot and encodes each
 * frame as a scaled JPEG. Frames are handed to a callback (the WebSocket
 * client) that ships them to the backend as base64 text messages.
 */
public class ScreenCaptureThread extends Thread {

    public interface FrameListener {
        void onFrame(String base64Jpeg);
    }

    private volatile boolean running;
    private volatile double scale = 1.0;
    private volatile int intervalMs = 120; // ~8 fps
    private final FrameListener listener;

    public ScreenCaptureThread(FrameListener listener) {
        super("screen-capture");
        this.listener = listener;
        setDaemon(true);
    }

    public void setQuality(double newScale, int newIntervalMs) {
        this.scale = Math.max(0.2, Math.min(1.0, newScale));
        this.intervalMs = Math.max(50, newIntervalMs);
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        running = true;
        try {
            Robot robot = new Robot();
            Rectangle bounds = virtualScreenBounds();
            while (running) {
                long start = System.currentTimeMillis();
                try {
                    BufferedImage shot = robot.createScreenCapture(bounds);
                    if (scale < 0.999) {
                        int w = Math.max(1, (int) (shot.getWidth() * scale));
                        int h = Math.max(1, (int) (shot.getHeight() * scale));
                        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = resized.createGraphics();
                        g.drawImage(shot, 0, 0, w, h, null);
                        g.dispose();
                        shot = resized;
                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ImageIO.write(shot, "jpg", out);
                    listener.onFrame(Base64.getEncoder().encodeToString(out.toByteArray()));
                } catch (Exception frameError) {
                    // Skip the failed frame and keep streaming.
                }
                long elapsed = System.currentTimeMillis() - start;
                Thread.sleep(Math.max(10, intervalMs - elapsed));
            }
        } catch (Exception e) {
            running = false;
        }
    }

    private static Rectangle virtualScreenBounds() {
        Rectangle virtual = new Rectangle(0, 0, 0, 0);
        for (java.awt.GraphicsDevice device : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getScreenDevices()) {
            for (java.awt.GraphicsConfiguration config : device.getConfigurations()) {
                virtual = virtual.union(config.getBounds());
            }
        }
        return virtual;
    }
}
