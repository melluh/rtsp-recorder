package com.melluh.rtsprecorder;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.melluh.rtsprecorder.http.RecordingsRoute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class RtspRecorder {

	public static final Logger LOGGER = Logger.getLogger("rtsp-recorder");
	private static RtspRecorder instance;
	
	private CameraRegistry cameraRegistry;
	private ConfigHandler configHandler;
	private Watchdog watchdog;
	
	private void start() {
		LOGGER.info("Starting rtsp-recorder...");
		
		this.cameraRegistry = new CameraRegistry();
		this.configHandler = new ConfigHandler();
		
		if(!configHandler.load()) {
			LOGGER.severe("Config read failed, exiting.");
			return;
		}
		
		LOGGER.info("Finishing loading config, " + cameraRegistry.getNumCameras() + " camera(s) defined");
		
		Vertx vertx = Vertx.vertx();
		HttpServer server = vertx.createHttpServer();
		Router router = Router.router(vertx);
		
		router.get("/recordings/*").handler(StaticHandler.create("recordings"));
		router.get("/api/recordings").blockingHandler(new RecordingsRoute());
		
		server.requestHandler(router).listen(configHandler.getWebPort());
		LOGGER.info("Web server listening on port " + server.actualPort());
		
		LOGGER.info("Starting FFmpeg processes...");
		
		cameraRegistry.getCameras().forEach(Camera::startProcess);
		this.watchdog = new Watchdog();
		watchdog.start();
		
		LOGGER.info("FFmpeg started and watchdog initialized.");
	}
	
	public CameraRegistry getCameraRegistry() {
		return cameraRegistry;
	}
	
	public ConfigHandler getConfigHandler() {
		return configHandler;
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
	
	public static RtspRecorder getInstance() {
		return instance;
	}
}
