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
	private File recordingsFolder;
	private int recordingsInterval;
	private int recordingsMaxSize;
	
	public boolean load() {
		File file = new File(FILENAME);
		if(!file.exists()) {
			RtspRecorder.LOGGER.severe(FILENAME + " is missing");
			return false;
		}
		
		try {
			String str = Files.readString(file.toPath());
			JsonObject json = new JsonObject(str);
			
			this.webPort = this.getInt(json, "web.port", 8080);
			this.recordingsFolder = new File(this.getString(json, "recordings.location", "recordings"));
			this.recordingsInterval = this.getInt(json, "recordings.interval", 600);
			this.recordingsMaxSize = this.getInt(json, "recordings.max_size_gb", -1);
			
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
					
					Camera camera = new Camera(name.toLowerCase(), url, jsonCamera.getLong("timeout_ms", 10000L));
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
	
	private int getInt(JsonObject json, String key, int def) {
		String[] parts = key.split("\\.");
		
		for(int i = 0; i < parts.length - 1; i++) {
			json = json.getJsonObject(parts[i]);
			if(json == null)
				return def;
		}
		
		return json.getInteger(parts[parts.length - 1], def);
	}
	
	private String getString(JsonObject json, String key, String def) {
		String[] parts = key.split("\\.");
		
		for(int i = 0; i < parts.length - 1; i++) {
			json = json.getJsonObject(parts[i]);
			if(json == null)
				return def;
		}
		
		return json.getString(parts[parts.length - 1], def);
	}
	
	public int getWebPort() {
		return webPort;
	}
	
	public File getRecordingsFolder() {
		return ensureDir(recordingsFolder);
	}
	
	public File getTempRecordingsFolder() {
		return ensureDir(new File(recordingsFolder, "temp"));
	}
	
	public int getRecordingsInterval() {
		return recordingsInterval;
	}
	
	public int getRecordingsMaxSize() {
		return recordingsMaxSize;
	}
	
	private File ensureDir(File dir) {
		if(!dir.isDirectory())
			dir.mkdirs();
		return dir;
	}
	
}
