package com.taobao.arthas.core.command.model;

/**
 * Result model for TimeCommand
 * 
 * @author example
 */
public class TimeModel extends ResultModel {
    
    private String currentTime;
    private String timezoneId;
    private String timezoneName;
    private boolean showTimezone;
    
    public TimeModel() {
    }
    
    public TimeModel(String currentTime, String timezoneId, String timezoneName, boolean showTimezone) {
        this.currentTime = currentTime;
        this.timezoneId = timezoneId;
        this.timezoneName = timezoneName;
        this.showTimezone = showTimezone;
    }
    
    @Override
    public String getType() {
        return "time";
    }
    
    public String getCurrentTime() {
        return currentTime;
    }
    
    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }
    
    public String getTimezoneId() {
        return timezoneId;
    }
    
    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }
    
    public String getTimezoneName() {
        return timezoneName;
    }
    
    public void setTimezoneName(String timezoneName) {
        this.timezoneName = timezoneName;
    }
    
    public boolean isShowTimezone() {
        return showTimezone;
    }
    
    public void setShowTimezone(boolean showTimezone) {
        this.showTimezone = showTimezone;
    }
}
