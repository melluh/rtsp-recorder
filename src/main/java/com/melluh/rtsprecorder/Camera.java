package com.melluh.rtsprecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Camera {

	private String name;
	private String url;
	
	private float fps;
	private float speed;
	private long lastUpdate;
	
	public Camera(String name, String url) {
		this.name = name;
		this.url = url;
	}
	
	public void startProcess() {
		try {
			Process process = new ProcessBuilder("ffmpeg", "-i", url, "-f", "segment", "-strftime", "1", "-segment_time", "600", "-segment_atclocktime", "1", "-segment_format", "mp4", "-an", "-vcodec", "copy", "-reset_timestamps", "1", "-progress", "pipe:1", "%Y-%m-%d-%H.%M.%S.mp4")
					.directory(this.getOutputDir())
					.redirectErrorStream(true)
					.start();
			
			new ProcessMonitorThread(process).start();
		} catch (IOException ex) {
			RtspRecorder.LOGGER.severe("Failed to start FFmpeg process for camera " + name);
			ex.printStackTrace();
		}
	}
	
	private class ProcessMonitorThread extends Thread {
		
		private Process process;
		
		public ProcessMonitorThread(Process process) {
			this.process = process;
		}
		
		@Override
		public void run() {
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while((line = reader.readLine()) != null) {
					String[] args = line.split("=");
					if(args.length != 2)
						continue;
					
					String key = args[0];
					String value = args[1];
					
					if(key.equals("fps")) {
						fps = Float.parseFloat(value);
						lastUpdate = System.currentTimeMillis();
					} else if(key.equals("speed")) {
						speed = Float.parseFloat(value.substring(0, value.length() - 1));
					}
				}
				
				RtspRecorder.LOGGER.info("FFmpeg process exited");
			} catch (IOException ex) {
				RtspRecorder.LOGGER.severe("Failed to read output from FFmpeg process for camera " + name);
				ex.printStackTrace();
			}
		}
		
	}
	
	private File getOutputDir() {
		File directory = new File("recordings/" + name);
		if(!directory.isDirectory()) {
			directory.mkdirs();
		}
		return directory;
	}
	
	public String getName() {
		return name;
	}
	
	public String getURL() {
		return url;
	}
	
	public float getFps() {
		return fps;
	}
	
	public float getSpeed() {
		return speed;
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}
	
}
