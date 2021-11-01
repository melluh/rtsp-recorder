package com.melluh.rtsprecorder;

import java.util.ArrayList;
import java.util.List;

public class CameraRegistry {

	private List<Camera> cameras = new ArrayList<>();
	
	public void registerCamera(Camera camera) {
		cameras.add(camera);
		RtspRecorder.LOGGER.info("Camera registered: " + camera.getName());
	}
	
	public List<Camera> getCameras() {
		return cameras;
	}

	public int getNumCameras() {
		return cameras.size();
	}
	
}
