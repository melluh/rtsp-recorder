package com.melluh.rtsprecorder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.vertx.core.json.JsonObject;

public class Recording {

	private String filePath;
	private String cameraName;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	
	public Recording(String filePath, String cameraName, LocalDateTime startTime, LocalDateTime endTime) {
		this.filePath = filePath;
		this.cameraName = cameraName;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public JsonObject toJson() {
		return new JsonObject()
				.put("filePath", filePath)
				.put("cameraName", cameraName)
				.put("startTime", startTime.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME))
				.put("endTime", endTime.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
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
	
}
