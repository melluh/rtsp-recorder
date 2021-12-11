package com.melluh.rtsprecorder;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;

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
	
	public File getFile() {
		return new File(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder(), filePath);
	}
	
	public JSONObject toJson() {
		return new JSONObject()
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
