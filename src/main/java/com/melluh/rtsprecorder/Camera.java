package com.melluh.rtsprecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
	
	public List<Recording> getRecordings() {
		List<Recording> recordings = new ArrayList<>();
		File directory = this.getOutputDir();
		
		for(File file : directory.listFiles()) {
			if(!file.isFile())
				continue;
			
			long startTime = FileUtil.getRecordingStart(file.getName());
			if(startTime <= 0)
				continue;
			
			float duration = FileUtil.getRecordingDuration(file);
			if(duration <= 0)
				continue;

			recordings.add(new Recording(file.getName(), startTime, (long) (startTime + (duration * 1000.0f))));
		}
		
		return recordings;
	}
	
	public static class Recording {
		
		private String name;
		private long from;
		private long to;
		
		public Recording(String name, long from, long to) {
			this.name = name;
			this.from = from;
			this.to = to;
		}
		
		public String getName() {
			return name;
		}
		
		public long getFrom() {
			return from;
		}
		
		public long getTo() {
			return to;
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
