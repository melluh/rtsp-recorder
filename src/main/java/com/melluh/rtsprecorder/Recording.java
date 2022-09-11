package com.melluh.rtsprecorder;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.grack.nanojson.JsonObject;

public class Recording {

	private final String filePath;
	private final String cameraName;
	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	
	public Recording(String filePath, String cameraName, LocalDateTime startTime, LocalDateTime endTime) {
		this.filePath = filePath;
		this.cameraName = cameraName;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public File getFile() {
		return new File(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder(), filePath);
	}
	
	public JsonObject toJson() {
		return JsonObject.builder()
				.value("filePath", filePath)
				.value("cameraName", cameraName)
				.value("startTime", startTime.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME))
				.value("endTime", endTime.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME))
				.done();
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public String getCameraName() {
		return cameraName;
	}
	
	public LocalDateTime getStartTime() {
		return startTime;
	}
	
	public LocalDateTime getEndTime() {
		return endTime;
	}

	public Duration getDuration() {
		return Duration.between(startTime, endTime);
	}
	
}
