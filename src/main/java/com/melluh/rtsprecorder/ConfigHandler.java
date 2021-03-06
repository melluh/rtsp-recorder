package com.melluh.rtsprecorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

public class ConfigHandler {
	
	private static final String FILENAME = "config.json";
	
	private int webPort;
	private File recordingsFolder;
	private int recordingsInterval;
	private int recordingsMaxSize;
	private int recordingsMaxSizeMargin;
	
	public boolean load() {
		File file = new File(FILENAME);
		if(!file.exists()) {
			Logger.error("{} is missing", FILENAME);
			return false;
		}
		
		try {
			String str = Files.readString(file.toPath());
			JSONObject json = new JSONObject(str);
			
			this.webPort = this.getInt(json, "web.port", 8080);
			this.recordingsFolder = new File(this.getString(json, "recordings.location", "recordings"));
			this.recordingsInterval = this.getInt(json, "recordings.interval", 600);
			this.recordingsMaxSize = this.getInt(json, "recordings.max_size_gb", -1);
			this.recordingsMaxSizeMargin = this.getInt(json, "recordings.max_size_margin_gb", 10);
			
			JSONArray jsonCameras = json.optJSONArray("cameras");
			if(jsonCameras != null) {
				for(int i = 0; i < jsonCameras.length(); i++) {
					JSONObject jsonCamera = jsonCameras.getJSONObject(i);
					
					String name = jsonCamera.getString("name");
					if(name == null) {
						Logger.error("Camera is missing value for 'name'");
						return false;
					}
					
					String url = jsonCamera.getString("url");
					if(url == null) {
						Logger.error("Camera is missing value for 'url'");
						return false;
					}
					
					Camera camera = new Camera(name.toLowerCase(), url, jsonCamera.optLong("timeout_ms", 10000L), jsonCamera.optLong("start_timeout_ms", 30000L));
					RtspRecorder.getInstance().getCameraRegistry().registerCamera(camera);
				}
			}
			
			return true;
		} catch (IOException ex) {
			Logger.error(ex, "Failed to read {}", FILENAME);
			return false;
		} catch (JSONException ex) {
			Logger.error(ex, "{} has malformed JSON.", FILENAME);
			return false;
		}
	}
	
	private int getInt(JSONObject json, String key, int def) {
		String[] parts = key.split("\\.");
		
		for(int i = 0; i < parts.length - 1; i++) {
			json = json.optJSONObject(parts[i]);
			if(json == null)
				return def;
		}
		
		return json.optInt(parts[parts.length - 1], def);
	}
	
	private String getString(JSONObject json, String key, String def) {
		String[] parts = key.split("\\.");
		
		for(int i = 0; i < parts.length - 1; i++) {
			json = json.optJSONObject(parts[i]);
			if(json == null)
				return def;
		}
		
		return json.optString(parts[parts.length - 1], def);
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
	
	public long getRecordingsMaxSize() {
		// convert gb -> bytes
		return recordingsMaxSize * 1073741824L;
	}
	
	public long getRecordingsMaxSizeMargin() {
		// convert gb -> bytes
		return recordingsMaxSizeMargin * 1073741824L;
	}
	
	private File ensureDir(File dir) {
		if(!dir.isDirectory())
			dir.mkdirs();
		return dir;
	}
	
}
