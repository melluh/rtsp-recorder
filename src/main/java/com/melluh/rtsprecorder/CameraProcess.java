package com.melluh.rtsprecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.melluh.rtsprecorder.util.FormatUtil;
import org.tinylog.Logger;

public class CameraProcess {
	
	private static final String COMMAND_FORMAT = "ffmpeg -hide_banner -i %input% -f segment -strftime 1 -segment_time 600 -segment_atclocktime 1 -segment_format mp4 -an -vcodec copy -reset_timestamps 1 -progress pipe:1 %file%";
	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	private static final Pattern OPENING_FOR_WRITING_PATTERN = Pattern.compile("Opening '(\\S+)' for writing");
	
	private Camera camera;
	
	private Process process;
	private volatile ProcessStatus status;
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
		if(status != ProcessStatus.IDLE) {
			this.isRestarting = true;
			this.stop(); // will restart process after it's stopped, because isRestarting=true
		} else {
			this.start(); // skip stopping, because the process isn't running
		}
	}
	
	public void handleTimeout(long timePassed) {
		if(status == ProcessStatus.STARTING) {
			if(failedStarts == 5) {
				this.failedStarts = 0;
				this.retryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2);
				camera.warn("FFmpeg startup timed out after {}. Failed to start 5 times, retrying in 2 minutes", FormatUtil.formatTimeTook(timePassed));
				return;
			}
			
			failedStarts++;
			camera.warn("FFmpeg startup timed out after {}, retrying (failed starts: {})", FormatUtil.formatTimeTook(timePassed), failedStarts);
			this.restart();
			return;
		}
		
		if(status == ProcessStatus.WORKING) {
			camera.warn("FFmpeg timed out after {}, restarting", FormatUtil.formatTimeTook(timePassed));
			this.restart();
			return;
		}
	}
	
	public void start() {
		if(status != ProcessStatus.IDLE) {
			camera.warn("Cannot start FFmpeg, status is {}", status.name());
			return;
		}
		
		if(process != null && process.isAlive()) {
			camera.warn("Cannot start FFmpeg, already running");
			return;
		}

		camera.info("Starting FFmpeg...");
		this.setStatus(ProcessStatus.STARTING);
		
		try {
			File tempFolder = new File(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder(), "temp");
			tempFolder.mkdirs();
			
			this.process = new ProcessBuilder(this.getCommand().split(" "))
					.directory(tempFolder)
					.redirectErrorStream(true)
					.start();
			
			process.onExit()
				.thenAccept(ph -> this.handleExit(ph.exitValue()));
			
			new LogReaderThread(process.getInputStream()).start();
		} catch (IOException ex) {
			camera.error(ex, "Error while starting FFmpeg");
		}
	}
	
	private void handleExit(long exitValue) {
		ProcessStatus prevStatus = this.status;
		
		this.setStatus(ProcessStatus.IDLE);
		this.process = null;
		this.activeFile = null;
		this.fps = 0;

		camera.info("FFmpeg quit with exit code {}, status was {}", exitValue, prevStatus.name());
		
		if(prevStatus != ProcessStatus.STOPPING) {
			camera.warn("Unexpected exit! Restarting process.");
			RtspRecorder.getInstance().getScheduler().schedule(this::start, 5, TimeUnit.SECONDS);
			return;
		}
		
		// otherwise if the status was STOPPING and we were restarting
		if(isRestarting) {
			isRestarting = false;
			RtspRecorder.getInstance().getScheduler().schedule(this::start, 2, TimeUnit.SECONDS);
			return;
		}
	}
	
	private String getCommand() {
		return COMMAND_FORMAT
				.replace("%input%", camera.getURL())
				.replace("%file%", camera.getName() + "-%Y-%m-%d-%H.%M.%S.mp4");
	}
	
	public void stop() {
		if(status != ProcessStatus.STARTING && status != ProcessStatus.WORKING) {
			camera.warn("Cannot stop FFmpeg, status is {}", status.name());
			return;
		}
		
		if(process == null || !process.isAlive()) {
			camera.warn("Cannot stop FFmpeg, not running", camera.getName());
			this.setStatus(ProcessStatus.IDLE);
			return;
		}
		
		ProcessStatus prevStatus = this.status;
		this.setStatus(ProcessStatus.STOPPING);
		
		// if the process was never working (so never writing to any files), just kill it right away
		if(prevStatus != ProcessStatus.WORKING) {
			process.destroyForcibly();
		} else {
			try {
				OutputStream out = process.getOutputStream();
				out.write("q".getBytes());
				out.flush();
				
				if(!process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
					camera.info("FFmpeg did not exit gracefully within {} seconds, killing process", SHUTDOWN_TIMEOUT_SECONDS);
					process.destroyForcibly();
				}
			} catch (IOException | InterruptedException ex) {
				camera.error(ex, "Failed to stop process");
			}
		}
		
		// Status will be set to IDLE by handleExit()
	}
	
	private void parseLog(String line) {
		if(line.startsWith("fps=")) {
			fps = line.equals("fps=N/A") ? 0 : Double.parseDouble(line.substring("fps=".length()));
			
			if(fps > 0) {
				lastUpdate = System.currentTimeMillis();
				
				if(status == ProcessStatus.STARTING) {
					camera.info("FFmpeg started successfully, took {}", FormatUtil.formatTimeTook(System.currentTimeMillis() - statusSince));
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
	
	public Camera getCamera() {
		return camera;
	}
	
	private void setStatus(ProcessStatus status) {
		this.status = status;
		this.statusSince = System.currentTimeMillis();
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
	
	public long getRetryTime() {
		return retryTime;
	}
	
	private class LogReaderThread extends Thread {
		
		private InputStream in;
		
		public LogReaderThread(InputStream in) {
			this.in = in;
		}
		
		@Override
		public void run() {
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				String line;
				while((line = reader.readLine()) != null) {
					parseLog(line);
				}
			} catch (IOException ex) {
				camera.error(ex, "Error while reading FFmpeg log output");
			}
		}
		
	}
	
	public enum ProcessStatus {
		
		IDLE, // Pre-startup, or after having shut down - process not running
		STARTING, // Starting up (process started but first update not received)
		WORKING, // Working, receiving updates
		STOPPING // Exiting
		
	}
	
}
