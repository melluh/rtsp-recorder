package com.melluh.rtsprecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraProcess {
	
	private static final String COMMAND_FORMAT = "ffmpeg -hide_banner -i %input% -f segment -strftime 1 -segment_time 600 -segment_atclocktime 1 -segment_format mp4 -an -vcodec copy -reset_timestamps 1 -progress pipe:1 %file%";
	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	private static final Pattern OPENING_FOR_WRITING_PATTERN = Pattern.compile("Opening '(\\S+)' for writing");
	
	private Camera camera;
	
	private Process process;
	private ProcessStatus status;
	private boolean isRestarting;
	
	private long lastUpdate;
	private long statusSince;
	private int failedStarts;
	private long retryTime;
	
	private String activeFile;
	private double fps;
	
	public CameraProcess(Camera camera) {
		Objects.requireNonNull(camera, "camera is missing");
		this.camera = camera;
		this.setStatus(ProcessStatus.IDLE);
	}
	
	public void restart() {
		this.isRestarting = true;
		this.stop(); // will restart process after it's stopped, because isRestarting=true
	}
	
	public void start() {
		if(status != ProcessStatus.IDLE) {
			RtspRecorder.LOGGER.warning("Cannot start process for camera " + camera.getName() + ", status is " + status.name());
			return;
		}
		
		if(process != null && process.isAlive()) {
			RtspRecorder.LOGGER.warning("Cannot start process for camera " + camera.getName() + ", process already running");
			return;
		}
		
		this.setStatus(ProcessStatus.STARTING);
		
		try {
			File tempFolder = new File(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder(), "temp");
			tempFolder.mkdirs();
			
			this.process = new ProcessBuilder(this.getCommand().split(" "))
					.directory(tempFolder)
					.redirectErrorStream(true)
					.start();
			
			new MonitorThread(process).start();
		} catch (IOException ex) {
			RtspRecorder.LOGGER.severe("Failed to start FFmpeg process for camera " + camera.getName());
			ex.printStackTrace();
		}
	}
	
	private String getCommand() {
		return COMMAND_FORMAT
				.replace("%input%", camera.getURL())
				.replace("%file%", camera.getName() + "-%Y-%m-%d-%H.%M.%S.mp4");
	}
	
	public void stop() {
		if(status != ProcessStatus.STARTING && status != ProcessStatus.WORKING) {
			RtspRecorder.LOGGER.warning("Cannot stop process for camera " + camera.getName() + ", status is " + status.name());
			return;
		}
		
		if(process == null || !process.isAlive()) {
			RtspRecorder.LOGGER.warning("Cannot stop process for camera " + camera.getName() + ", process not running");
			this.setStatus(ProcessStatus.IDLE);
			this.handleRestart();
			return;
		}
		
		boolean wasWorking = status == ProcessStatus.WORKING;
		this.setStatus(ProcessStatus.STOPPING);
		
		// if the process was never working (so never writing to any files), just kill it right away
		if(!wasWorking) {
			failedStarts++;
			process.destroyForcibly();
		} else {
			try {
				OutputStream out = process.getOutputStream();
				out.write("q".getBytes());
				out.flush();
				
				if(!process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
					RtspRecorder.LOGGER.info("FFmpeg for camera " + camera.getName() + " did not exit gracefully within " + SHUTDOWN_TIMEOUT_SECONDS + " seconds, killing process");
					process.destroyForcibly();
				}
			} catch (IOException | InterruptedException ex) {
				RtspRecorder.LOGGER.severe("Failed to stop process for camera " + camera.getName());
				ex.printStackTrace();
				return;
			}
		}
		
		process = null;
		this.setStatus(ProcessStatus.IDLE);
		this.resetAfterQuit();
		this.handleRestart();
		return;
	}
	
	private void parseLog(String line) {
		if(line.startsWith("fps=")) {
			fps = line.equals("fps=N/A") ? 0 : Double.parseDouble(line.substring("fps=".length()));
			
			if(fps > 0) {
				lastUpdate = System.currentTimeMillis();
				
				if(status == ProcessStatus.STARTING) {
					this.setStatus(ProcessStatus.WORKING);
					this.failedStarts = 0;
				}
			}
		}
		
		Matcher matcher = OPENING_FOR_WRITING_PATTERN.matcher(line);
		if(matcher.find()) {
			this.activeFile = matcher.group(1);
			return;
		}
	}
	
	private void handleRestart() {
		// if restarting, well.. restart
		if(isRestarting) {
			isRestarting = false;
			this.start();
		}
	}
	
	private void resetAfterQuit() {
		this.activeFile = null;
		this.fps = 0;
	}
	
	private void setStatus(ProcessStatus status) {
		this.status = status;
		this.statusSince = System.currentTimeMillis();
	}
	
	public Camera getCamera() {
		return camera;
	}
	
	public ProcessStatus getStatus() {
		return status;
	}
	
	public long getStatusSince() {
		return statusSince;
	}
	
	public int getFailedStarts() {
		return failedStarts;
	}
	
	public String getActiveFile() {
		return activeFile;
	}
	
	public long getPid() {
		return process != null ? process.pid() : 0;
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}
	
	public double getFps() {
		return fps;
	}
	
	public void setRetryTime(long retryTime) {
		this.retryTime = retryTime;
	}
	
	public long getRetryTime() {
		return retryTime;
	}
	
	private class MonitorThread extends Thread {
		
		private Process process;
		
		public MonitorThread(Process process) {
			super(camera.getName() + "-monitor");
			this.process = process;
		}
		
		@Override
		public void run() {
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while((line = reader.readLine()) != null) {
					parseLog(line);
				}
				
				if(status != ProcessStatus.STOPPING) {
					RtspRecorder.LOGGER.warning("Process for camera " + camera.getName() + " exited unexpectedly, restarting process");
					setStatus(ProcessStatus.IDLE);
					resetAfterQuit();
					CameraProcess.this.start();
				}
			} catch (IOException ex) {
				RtspRecorder.LOGGER.severe("Failed to read log output from FFmpeg process for camera " + camera.getName());
				ex.printStackTrace();
			}
		}
		
	}
	
	public static enum ProcessStatus {
		
		IDLE, // Pre-startup, or after having shut down
		STARTING, // Starting up (process started but first update not received)
		WORKING, // Working, receiving updates
		STOPPING // Exiting
		
	}
	
}
