package com.remotesupport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for application control commands.
 */
public class ApplicationControlRequest {
    @JsonProperty("action")
    private String action;  // focus, minimize, maximize, close, resize, click, rightClick, scroll, text, key

    @JsonProperty("hwnd")
    private Long hwnd;

    @JsonProperty("x")
    private Integer x;

    @JsonProperty("y")
    private Integer y;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("normalizedX")
    private Double normalizedX;  // For mouse clicks (0..1)

    @JsonProperty("normalizedY")
    private Double normalizedY;

    @JsonProperty("delta")
    private Integer delta;  // For scroll

    @JsonProperty("text")
    private String text;  // For text input

    @JsonProperty("keyCode")
    private Integer keyCode;  // For key presses

    @JsonProperty("shift")
    private Boolean shift;

    @JsonProperty("ctrl")
    private Boolean ctrl;

    @JsonProperty("alt")
    private Boolean alt;

    @JsonProperty("duration")
    private Integer duration;  // For drag operations

    @JsonProperty("searchTerm")
    private String searchTerm;  // For finding windows

    public ApplicationControlRequest() {}

    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getHwnd() { return hwnd; }
    public void setHwnd(Long hwnd) { this.hwnd = hwnd; }

    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }

    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Double getNormalizedX() { return normalizedX; }
    public void setNormalizedX(Double normalizedX) { this.normalizedX = normalizedX; }

    public Double getNormalizedY() { return normalizedY; }
    public void setNormalizedY(Double normalizedY) { this.normalizedY = normalizedY; }

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Integer getKeyCode() { return keyCode; }
    public void setKeyCode(Integer keyCode) { this.keyCode = keyCode; }

    public Boolean getShift() { return shift; }
    public void setShift(Boolean shift) { this.shift = shift; }

    public Boolean getCtrl() { return ctrl; }
    public void setCtrl(Boolean ctrl) { this.ctrl = ctrl; }

    public Boolean getAlt() { return alt; }
    public void setAlt(Boolean alt) { this.alt = alt; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
}
