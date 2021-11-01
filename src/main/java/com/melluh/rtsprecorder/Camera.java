package com.melluh.rtsprecorder;

public class Camera {

	private String name;
	private String url;
	
	public Camera(String name, String url) {
		this.name = name;
		this.url = url;
	}
	
	public String getName() {
		return name;
	}
	
	public String getURL() {
		return url;
	}
	
}
