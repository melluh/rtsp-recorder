package com.melluh.rtsprecorder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.melluh.rtsprecorder.http.WebServer;
import com.melluh.rtsprecorder.task.CleanupRecordingsTask;
import com.melluh.rtsprecorder.task.MoveRecordingsTask;
import com.melluh.rtsprecorder.task.WatchdogTask;
import org.tinylog.Logger;
import org.tinylog.provider.ProviderRegistry;

public class RtspRecorder {

	private static RtspRecorder instance;
	
	public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
	public static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(3);
	
	private CameraRegistry cameraRegistry;
	private ConfigHandler configHandler;
	private Database database;

	private void start() {
		Logger.info("Starting rtsp-recorder...");
		
		this.cameraRegistry = new CameraRegistry();
		this.configHandler = new ConfigHandler();
		
		if(!configHandler.load()) {
			Logger.error("Config read failed, exiting.");
			return;
		}

		Logger.info("Finishing loading config, " + cameraRegistry.getNumCameras() + " camera(s) defined");
		Logger.info("Recordings will be stored in " + configHandler.getRecordingsFolder().getAbsolutePath());
		
		this.database = new Database();
		database.connect();

		WebServer webServer = new WebServer();
		webServer.start();

		Logger.info("Starting FFmpeg processes...");
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
		cameraRegistry.getCameras().forEach(camera -> camera.getProcess().start());
		Logger.info("FFmpeg processed started.");

		SCHEDULED_EXECUTOR.scheduleAtFixedRate(new WatchdogTask(), 1, 1, TimeUnit.SECONDS);
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(new MoveRecordingsTask(), 0, 5, TimeUnit.MINUTES);
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(new CleanupRecordingsTask(), 0, 30, TimeUnit.MINUTES);
		Logger.info("Tasks initialized.");
	}
	
	private class ShutdownHook extends Thread {
		
		@Override
		public void run() {
			Logger.info("Stopping FFmpeg processes...");
			cameraRegistry.getCameras().forEach(camera -> camera.getProcess().stop());

			Logger.info("Goodbye!");

			// Shutdown tinylog
			try {
				ProviderRegistry.getLoggingProvider().shutdown();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	public CameraRegistry getCameraRegistry() {
		return cameraRegistry;
	}
	
	public ConfigHandler getConfigHandler() {
		return configHandler;
	}

	public Database getDatabase() {
		return database;
	}
	
	public static void main(String[] args) {
		instance = new RtspRecorder();
		instance.start();
	}
	
	public static RtspRecorder getInstance() {
		return instance;
	}
}
