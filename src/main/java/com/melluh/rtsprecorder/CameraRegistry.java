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

	public Camera getCamera(String name) {
		for(Camera camera : cameras) {
			if(camera.getName().equalsIgnoreCase(name))
				return camera;
		}
		
		return null;
	}
	
	public boolean isInProgressFile(String name) {
		return cameras.stream()
				.anyMatch(camera -> camera.getFileInProgress() != null && camera.getFileInProgress().equals(name));
	}
	
}
