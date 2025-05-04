package com.melluh.rtsprecorder.http;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.grack.nanojson.JsonWriter;
import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.simplehttpserver.HttpServer;
import com.melluh.simplehttpserver.protocol.MimeType;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;
import com.melluh.simplehttpserver.router.Router;
import org.tinylog.Logger;

public class WebServer {
	
	public void start() {
		File webroot = new File("webroot");

		try {
			Files.createDirectories(webroot.toPath());
		} catch (IOException ex) {
			Logger.error(ex, "Failed to create webroot directory");
			return;
		}

		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		
		try {
			new HttpServer(configHandler.getWebPort())
					.use(new Router()
							.get("/status", new StatusRoute())
							.get("/recordings", new RecordingsRoute())
							.get("/clips", new ClipsRoute())
							.post("/clips/create", new ClipsCreateRoute())
					)
					.start();

			Logger.info("Web server listening on port {}", configHandler.getWebPort());
		} catch (IOException ex) {
			Logger.error(ex, "Failed to start web server");
		}
	}

	public static Response jsonResponse(Status status, Object json) {
		return new Response(status)
				.contentType(MimeType.JSON)
				.body(JsonWriter.string(json));
	}
	
}
