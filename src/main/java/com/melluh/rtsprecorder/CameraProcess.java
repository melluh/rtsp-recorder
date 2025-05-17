package com.melluh.rtsprecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.melluh.rtsprecorder.util.FormatUtil;

public class CameraProcess {
	
	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	private static final Pattern OPENING_FOR_WRITING_PATTERN = Pattern.compile("Opening '(\\S+)' for writing");
	
	private final Camera camera;
	
	private Process process;
	private volatile ProcessStatus status;
	private boolean isRestarting;

	private long lastTimeUs;
	private long lastUpdate;
	private String activeFile;

	private long statusSince;
	private int failedStarts;
	private long retryTime;
	
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
			camera.warn("FFmpeg has been stuck for {}, restarting", FormatUtil.formatTimeTook(timePassed));
			this.restart();
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
			Files.createDirectories(tempFolder.toPath());
			
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
		this.lastTimeUs = 0;

		camera.info("FFmpeg quit with exit code {}, status was {}", exitValue, prevStatus.name());
		
		if(prevStatus != ProcessStatus.STOPPING) {
			camera.warn("Unexpected exit! Restarting process.");
			RtspRecorder.SCHEDULED_EXECUTOR.schedule(this::start, 5, TimeUnit.SECONDS);
			return;
		}
		
		// otherwise if the status was STOPPING and we were restarting
		if(isRestarting) {
			isRestarting = false;
			RtspRecorder.SCHEDULED_EXECUTOR.schedule(this::start, 2, TimeUnit.SECONDS);
		}
	}
	
	private String getCommand() {
		return RtspRecorder.getInstance().getConfigHandler().getFfmpegCommand()
				.replace("%input%", camera.getURL())
				.replace("%name%", camera.getName())
				.replace("%interval%", String.valueOf(RtspRecorder.getInstance().getConfigHandler().getRecordingsInterval()));
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
		if (line.startsWith("out_time_us=")) {
			long timeUs = Math.max(Long.parseLong(line.substring("out_time_us=".length())), 0);
			if (timeUs > lastTimeUs) {
				this.lastTimeUs = timeUs;
				this.lastUpdate = System.currentTimeMillis();

				if (status == ProcessStatus.STARTING) {
					camera.info("FFmpeg started successfully, took {}", FormatUtil.formatTimeTook(System.currentTimeMillis() - statusSince));
					this.setStatus(ProcessStatus.WORKING);
					this.failedStarts = 0;
				}
			}
		}

		Matcher matcher = OPENING_FOR_WRITING_PATTERN.matcher(line);
		if (matcher.find()) {
			String currentFile = this.activeFile;
			this.activeFile = matcher.group(1);

			if (currentFile != null) {
				File file = new File(RtspRecorder.getInstance().getConfigHandler().getTempRecordingsFolder(), currentFile);
				RtspRecorder.EXECUTOR.execute(() -> FileHandler.process(file));
			}
		}

		if (RtspRecorder.getInstance().getConfigHandler().isFfmpegToConsole())
			camera.info(line);
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
	
	public long getRetryTime() {
		return retryTime;
	}
	
	private class LogReaderThread extends Thread {
		
		private final InputStream in;
		
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
