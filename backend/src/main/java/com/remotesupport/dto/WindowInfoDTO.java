package com.remotesupport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for window/application information.
 */
public class WindowInfoDTO {
    @JsonProperty("hwnd")
    private long hwnd;

    @JsonProperty("title")
    private String title;

    @JsonProperty("className")
    private String className;

    @JsonProperty("x")
    private int x;

    @JsonProperty("y")
    private int y;

    @JsonProperty("width")
    private int width;

    @JsonProperty("height")
    private int height;

    @JsonProperty("visible")
    private boolean visible;

    @JsonProperty("processId")
    private int processId;

    public WindowInfoDTO() {}

    public WindowInfoDTO(long hwnd, String title, String className, int x, int y, 
                         int width, int height, boolean visible, int processId) {
        this.hwnd = hwnd;
        this.title = title;
        this.className = className;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = visible;
        this.processId = processId;
    }

    // Getters and Setters
    public long getHwnd() { return hwnd; }
    public void setHwnd(long hwnd) { this.hwnd = hwnd; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public int getProcessId() { return processId; }
    public void setProcessId(int processId) { this.processId = processId; }
}
