package com.melluh.rtsprecorder.http;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.simplehttpserver.HttpServer;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.RequestHandler;
import com.melluh.simplehttpserver.response.Response;

public class WebServer {
	
	public void start() {
		File webroot = new File("webroot");
		webroot.mkdir();
		
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		StaticFileRoute recordingsStatic = new StaticFileRoute(configHandler.getRecordingsFolder(), "/recordings/");
		StaticFileRoute webStatic = new StaticFileRoute(webroot, "/").serveIndex(true);
		
		try {
			new HttpServer(configHandler.getWebPort())
				.requestHandler(new RequestHandler() {
					@Override
					public Response handle(Request req) {
						if(req.getLocation().equals("/api/status")) {
							return StatusRoute.handle(req);
						}
						
						if(req.getLocation().equals("/api/recordings")) {
							return RecordingsRoute.handle(req);
						}
						
						if(req.getLocation().startsWith("/recordings/")) {
							return recordingsStatic.handle(req);
						}
						
						return webStatic.handle(req);
					}
				})
				.start();
			
			RtspRecorder.LOGGER.info("Web server listening on port " + configHandler.getWebPort());
		} catch (IOException ex) {
			RtspRecorder.LOGGER.log(Level.SEVERE, "Failed to start web server", ex);
		}
		
		//router.get().handler(StaticHandler.create().setCachingEnabled(false).setFilesReadOnly(false));
		//router.get("/recordings/*").handler(StaticHandler.create("recordings").setFilesReadOnly(false));
	}
	
}
