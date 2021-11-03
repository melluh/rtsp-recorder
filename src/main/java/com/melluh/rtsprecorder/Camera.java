package com.melluh.rtsprecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.melluh.rtsprecorder.util.FormatUtil;

public class Camera {
	
	private static final Pattern OPENING_FOR_WRITING_PATTERN = Pattern.compile("Opening '(\\S+)' for writing");
	
	private String name;
	private String url;
	private long timeout;
	
	private float fps;
	private float speed;
	
	private long connectedSince;
	private long lastUpdate;
	private boolean isWorking, isRestarting;
	private int failedStarts;
	private long retryStartTime;
	
	private Process process;
	private String fileInProgress;
	
	public Camera(String name, String url, long timeout) {
		this.name = name;
		this.url = url;
		this.timeout = timeout;
	}
	
	public void restartProcess() {
		this.isRestarting = true;
		
		if(this.isProcessRunning()) {
			this.stopProcess();
		}
		
		this.startProcess();
	}
	
	public void stopProcess() {
		if(!this.isProcessRunning())
			throw new IllegalStateException();
		
		// if not writing to any files, just kill the process right away
		if(fileInProgress == null) {
			failedStarts++;
			process.destroyForcibly();
			process = null;
			return;
		}
		
		try {
			// Send 'q', which should cause FFmpeg to exit
			OutputStream out = process.getOutputStream();
			out.write("q".getBytes());
			out.flush();
			
			if(!process.waitFor(5, TimeUnit.SECONDS)) {
				RtspRecorder.LOGGER.warning("FFmpeg for camera " + name + " did not exit gracefully within 5 seconds, kiling process");
				process.destroyForcibly();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		process = null;
	}
	
	public void startProcess() {
		if(this.isProcessRunning())
			throw new IllegalStateException();
		
		this.isWorking = false;
		this.isRestarting = false;
		this.connectedSince = 0;
		this.fileInProgress = null;
		
		try {
			File tempFolder = new File(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder(), "temp");
			tempFolder.mkdirs();
			String fileName = name + "-%Y-%m-%d-%H.%M.%S.mp4";
			
			this.process = new ProcessBuilder("ffmpeg", "-i", url, "-f", "segment", "-strftime", "1", "-segment_time", "600", "-segment_atclocktime", "1", "-segment_format", "mp4", "-an", "-vcodec", "copy", "-reset_timestamps", "1", "-progress", "pipe:1", fileName)
					.directory(tempFolder)
					.redirectErrorStream(true)
					.start();
			
			new ProcessMonitorThread(process).start();
			this.lastUpdate = System.currentTimeMillis();
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
					if(this.tryParseValue(line))
						continue;
					
					Matcher matcher = OPENING_FOR_WRITING_PATTERN.matcher(line);
					if(matcher.find()) {
						fileInProgress = matcher.group(1);
					}
				}
				
				// ffmpeg process stopped
				isWorking = false;
				connectedSince = 0;
				fileInProgress = null;
			} catch (IOException ex) {
				RtspRecorder.LOGGER.severe("Failed to read output from FFmpeg process for camera " + name);
				ex.printStackTrace();
			}
		}
		
		private boolean tryParseValue(String line) {
			String[] args = line.split("=");
			if(args.length != 2)
				return false;
			
			String key = args[0];
			String value = args[1];
			
			if(key.equals("fps")) {
				fps = FormatUtil.parseFloat(value);
				lastUpdate = System.currentTimeMillis();
				
				isWorking = true;
				failedStarts = 0;
				if(connectedSince <= 0)
					connectedSince = System.currentTimeMillis();
			} else if(key.equals("speed")) {
				speed = value.equals("N/A") ? 0 : FormatUtil.parseFloat(value.substring(0, value.length() - 1));
			}
			
			return true;
		}
		
	}
	
	public String getName() {
		return name;
	}
	
	public String getURL() {
		return url;
	}
	
	public long getTimeout() {
		return timeout;
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
	
	public boolean isProcessRunning() {
		return process != null && process.isAlive();
	}
	
	public boolean isWorking() {
		return isWorking;
	}
	
	public long getConnectedSince() {
		return connectedSince;
	}
	
	public String getFileInProgress() {
		return fileInProgress;
	}
	
	public long getPid() {
		return process != null ? process.pid() : 0;
	}
	
	public boolean isRestarting() {
		return isRestarting;
	}
	
	public int getFailedStarts() {
		return failedStarts;
	}
	
	public void setRetryStartTime(long retryStartTime) {
		this.retryStartTime = retryStartTime;
		this.failedStarts = 0;
	}
	
	public long getRetryStartTime() {
		return retryStartTime;
	}
	
}
