package com.melluh.rtsprecorder;

import com.grack.nanojson.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Clip extends Recording {

    private final String label;
    private final LocalDateTime savedTime;

    public Clip(String filePath, String label, String cameraName, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime savedTime) {
        super(filePath, cameraName, startTime, endTime);
        this.label = label;
        this.savedTime = savedTime;
    }

    @Override
    public JsonObject toJson() {
        return JsonObject.builder()
                .value("filePath", this.getFilePath())
                .value("label", label)
                .value("cameraName", this.getCameraName())
                .value("startTime", this.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .value("endTime", this.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .value("savedTime", savedTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .done();
    }

    public String getLabel() {
        return label;
    }

    public LocalDateTime getSavedTime() {
        return savedTime;
    }

}
