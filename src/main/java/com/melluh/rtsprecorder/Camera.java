package com.melluh.rtsprecorder;

public class Camera {
	
	private String name;
	private String url;
	private long timeout;
	
	private CameraProcess process;
	
	public Camera(String name, String url, long timeout) {
		this.name = name;
		this.url = url;
		this.timeout = timeout;
		this.process = new CameraProcess(this);
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
	
}
