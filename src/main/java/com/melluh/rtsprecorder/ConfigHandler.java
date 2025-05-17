package com.melluh.rtsprecorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.tinylog.Logger;

public class ConfigHandler {
	
	private static final String FILENAME = "config.json";
	
	private int webPort;
	private File recordingsFolder;
	private int recordingsInterval;
	private int recordingsMaxSize;
	private int recordingsMaxSizeMargin;
	private String ffmpegCommand;
	private boolean ffmpegToConsole;
	
	public boolean load() {
		File file = new File(FILENAME);
		if(!file.exists()) {
			Logger.error("{} is missing", FILENAME);
			return false;
		}
		
		try {
			String str = Files.readString(file.toPath());
			JsonObject json = JsonParser.object().from(str);

			this.webPort = json.getObject("web").getInt("port", 8080);
			this.recordingsFolder = new File(json.getObject("recordings").getString("location", "recordings"));
			this.recordingsInterval = json.getObject("recordings").getInt("interval", 600);
			this.recordingsMaxSize = json.getObject("recordings").getInt("max_size_gb", -1);
			this.recordingsMaxSizeMargin = json.getObject("recordings").getInt("max_size_margin_gb", 10);
			this.ffmpegCommand = json.getString("ffmpeg_command");
			this.ffmpegToConsole = json.getBoolean("ffmpeg_to_console");
			
			JsonArray jsonCameras = json.getArray("cameras");
			if(jsonCameras != null) {
				for(int i = 0; i < jsonCameras.size(); i++) {
					JsonObject jsonCamera = jsonCameras.getObject(i);
					
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
					
					Camera camera = new Camera(name.toLowerCase(), url, jsonCamera.getLong("timeout_ms", 10000L), jsonCamera.getLong("start_timeout_ms", 30000L));
					RtspRecorder.getInstance().getCameraRegistry().registerCamera(camera);
				}
			}
			
			return true;
		} catch (IOException ex) {
			Logger.error(ex, "Failed to read {}", FILENAME);
			return false;
		} catch (JsonParserException ex) {
			Logger.error(ex, "{} has malformed JSON.", FILENAME);
			return false;
		}
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

	public String getFfmpegCommand() {
		return ffmpegCommand;
	}

	public boolean isFfmpegToConsole() {
		return ffmpegToConsole;
	}

	private File ensureDir(File dir) {
		if(!dir.isDirectory())
			dir.mkdirs();
		return dir;
	}
	
}
