package com.melluh.rtsprecorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ConfigHandler {
	
	private static final String FILENAME = "config.json";
	
	private int webPort;
	
	public boolean load() {
		File file = new File(FILENAME);
		if(!file.exists()) {
			RtspRecorder.LOGGER.severe(FILENAME + " is missing");
			return false;
		}
		
		try {
			String str = Files.readString(file.toPath());
			JsonObject json = new JsonObject(str);
			
			this.webPort = json.getInteger("web.port", 8080);
			
			JsonArray jsonCameras = json.getJsonArray("cameras");
			if(jsonCameras != null) {
				for(int i = 0; i < jsonCameras.size(); i++) {
					JsonObject jsonCamera = jsonCameras.getJsonObject(i);
					
					String name = jsonCamera.getString("name");
					if(name == null) {
						RtspRecorder.LOGGER.severe("Camera is missing value for 'name'");
						return false;
					}
					
					String url = jsonCamera.getString("url");
					if(url == null) {
						RtspRecorder.LOGGER.severe("Camera is missing value for 'url'");
						return false;
					}
					
					Camera camera = new Camera(name, url, jsonCamera.getLong("timeout", 10000L));
					RtspRecorder.getInstance().getCameraRegistry().registerCamera(camera);
				}
			}
			
			return true;
		} catch (IOException ex) {
			RtspRecorder.LOGGER.severe("Failed to read " + FILENAME);
			ex.printStackTrace();
			return false;
		} catch (DecodeException ex) {
			RtspRecorder.LOGGER.severe(FILENAME + " has malformed JSON.");
			ex.printStackTrace();
			return false;
		}
	}
	
	public int getWebPort() {
		return webPort;
	}
	
}
