package com.melluh.rtsprecorder;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.melluh.rtsprecorder.http.RecordingsRoute;
import com.melluh.rtsprecorder.http.StatusRoute;
import com.melluh.rtsprecorder.task.CleanupRecordingsTask;
import com.melluh.rtsprecorder.task.MoveRecordingsTask;
import com.melluh.rtsprecorder.task.WatchdogTask;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class RtspRecorder {

	static {
		System.setProperty("java.util.logging.manager", CustomLogManager.class.getName());
	}
	
	public static final Logger LOGGER = Logger.getLogger("rtsp-recorder");
	private static RtspRecorder instance;
	
	private final ExecutorService threadPool = Executors.newCachedThreadPool();
	
	private CameraRegistry cameraRegistry;
	private ConfigHandler configHandler;
	private Database database;
	
	private void start() {
		LOGGER.info("Starting rtsp-recorder...");
		
		this.cameraRegistry = new CameraRegistry();
		this.configHandler = new ConfigHandler();
		
		if(!configHandler.load()) {
			LOGGER.severe("Config read failed, exiting.");
			return;
		}
		
		LOGGER.info("Finishing loading config, " + cameraRegistry.getNumCameras() + " camera(s) defined");
		LOGGER.info("Recordings will be stored in " + configHandler.getRecordingsFolder().getAbsolutePath());
		
		this.database = new Database();
		database.connect();
		
		Vertx vertx = Vertx.vertx();
		HttpServer server = vertx.createHttpServer();
		Router router = Router.router(vertx);
		
		router.get().handler(StaticHandler.create().setCachingEnabled(false).setFilesReadOnly(false));
		router.get("/recordings/*").handler(StaticHandler.create("recordings").setFilesReadOnly(false));
		router.get("/api/recordings").blockingHandler(new RecordingsRoute());
		router.get("/api/status").handler(new StatusRoute());
		
		server.requestHandler(router).listen(configHandler.getWebPort());
		LOGGER.info("Web server listening on port " + server.actualPort());
		
		LOGGER.info("Starting FFmpeg processes...");
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
		cameraRegistry.getCameras().forEach(Camera::startProcess);
		LOGGER.info("FFmpeg processed started.");
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
		executor.scheduleAtFixedRate(new WatchdogTask(), 1, 1, TimeUnit.SECONDS);
		executor.scheduleAtFixedRate(new MoveRecordingsTask(), 0, 5, TimeUnit.MINUTES);
		executor.scheduleAtFixedRate(new CleanupRecordingsTask(), 0, 30, TimeUnit.MINUTES);
		LOGGER.info("Tasks initialized.");
	}
	
	private class ShutdownHook extends Thread {
		
		@Override
		public void run() {
			LOGGER.info("Stopping FFmpeg processes...");
			cameraRegistry.getCameras().forEach(Camera::stopProcess);
			
			LOGGER.info("Goodbye!");
			CustomLogManager.resetFinally();
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
	
	public ExecutorService getThreadPool() {
		return threadPool;
	}
	
	public static void main(String[] args) {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter() {
			private static final String FORMAT = "[%1$tF %1$tT] [%2$s] %3$s %n";
			
			@Override
			public synchronized String format(LogRecord record) {
				return String.format(FORMAT, new Date(record.getMillis()), record.getLevel().getName(), record.getMessage());
			}
		});
		LOGGER.setUseParentHandlers(false);
		LOGGER.addHandler(handler);
		
		instance = new RtspRecorder();
		instance.start();
	}
	
	// this is needed to make sure logging keeps working in the shutdown hook
	public static class CustomLogManager extends LogManager {
		
		private static CustomLogManager instance;
		
		public CustomLogManager() {
			instance = this;
		}
		
		@Override
		public void reset() throws SecurityException {}
		
		private void resetSuper() {
			super.reset();
		}
		
		public static void resetFinally() {
			instance.resetSuper();
		}
		
	}
	
	public static RtspRecorder getInstance() {
		return instance;
	}
}
