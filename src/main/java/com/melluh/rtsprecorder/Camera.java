package com.melluh.rtsprecorder;

import java.util.logging.Level;

public class Camera {
	
	private String name;
	private String url;
	private long timeout;
	private long startTimeout;
	
	private CameraProcess process;
	
	public Camera(String name, String url, long timeout, long startTimeout) {
		this.name = name;
		this.url = url;
		this.timeout = timeout;
		this.startTimeout = startTimeout;
		this.process = new CameraProcess(this);
	}
	
	public void log(Level level, String msg) {
		RtspRecorder.LOGGER.log(level, name + ": " + msg);
	}
	
	public CameraProcess getProcess() {
		return process;
	}
	
	public String getName() {
		return name;
	}
	
	public String getURL() {
		return url;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public long getStartTimeout() {
		return startTimeout;
	}
	
}
