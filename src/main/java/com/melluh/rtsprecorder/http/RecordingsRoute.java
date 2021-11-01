package com.melluh.rtsprecorder.http;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.Camera.Recording;
import com.melluh.rtsprecorder.RtspRecorder;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class RecordingsRoute implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext ctx) {
		JsonObject jsonCameras = new JsonObject();
		for(Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			JsonArray jsonRecordings = new JsonArray();
			jsonCameras.put(camera.getName(), jsonRecordings);
			
			for(Recording recording : camera.getRecordings()) {
				JsonObject jsonRecording = new JsonObject();
				jsonRecording.put("url", "/recordings/" + recording.getName());
				jsonRecording.put("from", recording.getFrom());
				jsonRecording.put("to", recording.getTo());
				jsonRecordings.add(jsonRecording);
			}
		}
		
		ctx.json(jsonCameras);
	}

}
