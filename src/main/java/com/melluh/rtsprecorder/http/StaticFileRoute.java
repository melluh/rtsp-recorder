package com.melluh.rtsprecorder.http;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.simplehttpserver.HttpUtils;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;

public class StaticFileRoute {

	private static final String INDEX_FILE = "index.html";
	
	private File folder;
	private String baseUri;
	private boolean serveIndex;
	
	public StaticFileRoute(File folder) {
		this.folder = folder;
		this.baseUri = "/";
	}
	
	public StaticFileRoute(File folder, String baseUri) {
		this.folder = folder;
		this.baseUri = baseUri;
	}
	
	public StaticFileRoute serveIndex(boolean serveIndex) {
		this.serveIndex = serveIndex;
		return this;
	}
	
	public Response handle(Request req) {
		if(!req.getLocation().startsWith(baseUri))
			return new Response(Status.NOT_FOUND);
		
		String location = req.getLocation().substring(baseUri.length());
		
		File file = new File(folder, location);
		if(!file.isFile()) {
			if(!serveIndex) {
				return new Response(Status.NOT_FOUND);
			}
			
			file = new File(folder, INDEX_FILE);
			if(!file.isFile()) {
				return new Response(Status.NOT_FOUND);
			}
		}
		
		try {
			return HttpUtils.serveFile(req, file);
		} catch (IOException ex) {
			RtspRecorder.LOGGER.log(Level.SEVERE, "Failed to serve static file " + file.getName(), ex);
			return new Response(Status.INTERNAL_SERVER_ERROR);
		}
	}

}
