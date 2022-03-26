package com.melluh.rtsprecorder;

import org.tinylog.Logger;

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

	public void info(String msg, Object... args) {
		Logger.info(this.name + ": " + msg, args);
	}

	public void warn(String msg, Object... args) {
		Logger.warn(this.name + ": " + msg, args);
	}

	public void error(Throwable ex, String msg, Object... args) {
		Logger.error(ex, this.name + ": " + msg, args);
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
