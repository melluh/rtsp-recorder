package com.melluh.rtsprecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CameraRegistry {

	private List<Camera> cameras = new ArrayList<>();
	
	public void registerCamera(Camera camera) {
		cameras.add(camera);
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
	
	public boolean isActiveFile(String name) {
		return cameras.stream()
				.anyMatch(camera -> Objects.equals(camera.getProcess().getActiveFile(), name));
	}
	
}
