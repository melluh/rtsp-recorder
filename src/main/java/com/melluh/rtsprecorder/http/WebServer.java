package com.melluh.rtsprecorder.http;

import java.io.File;
import java.io.IOException;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;
import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.simplehttpserver.HttpServer;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.RequestHandler;
import com.melluh.simplehttpserver.protocol.MimeType;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;
import com.melluh.simplehttpserver.router.Router;
import org.tinylog.Logger;

public class WebServer {
	
	public void start() {
		File webroot = new File("webroot");
		webroot.mkdir();
		
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		StaticFileHandler recordingsStatic = new StaticFileHandler(configHandler.getRecordingsFolder(), "/recordings/").downloadQuery(true);
		StaticFileHandler webStatic = new StaticFileHandler(webroot, "/").serveIndex(true);
		
		try {
			new HttpServer(configHandler.getWebPort())
					.use(webStatic)
					.use(new Router()
							.get("/api/status", new StatusRoute())
							.get("/api/recordings", new RecordingsRoute())
							.get("/api/export", new ExportRoute())
							.get("/recordings/*", recordingsStatic)
					)
					.start();

			Logger.info("Web server listening on port {}", configHandler.getWebPort());
		} catch (IOException ex) {
			Logger.error(ex, "Failed to start web server");
		}
		
		//router.get().handler(StaticHandler.create().setCachingEnabled(false).setFilesReadOnly(false));
		//router.get("/recordings/*").handler(StaticHandler.create("recordings").setFilesReadOnly(false));
	}

	public static Response jsonResponse(Status status, Object json) {
		return new Response(status)
				.contentType(MimeType.JSON)
				.body(JsonWriter.string(json));
	}
	
}
