package com.melluh.rtsprecorder.http;

import java.time.LocalDate;
import java.util.List;

import com.melluh.rtsprecorder.Recording;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FormatUtil;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class RecordingsRoute implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext ctx) {
		LocalDate date = FormatUtil.parseDate(ctx.queryParams().get("date"));
		if(date == null) {
			ctx.response().setStatusCode(400).putHeader("Content-Type", "application/json").end(Json.encodeToBuffer(new JsonObject().put("success", false).put("message", "request is missing date")));
			return;
		}
		
		String cameraName = ctx.queryParams().get("camera");
		if(cameraName == null || cameraName.isEmpty()) {
			ctx.response().setStatusCode(400).putHeader("Content-Type", "application/json").end(Json.encodeToBuffer(new JsonObject().put("success", false).put("message", "request is missing camera")));
			return;
		}
		
		if(RtspRecorder.getInstance().getCameraRegistry().getCamera(cameraName) == null) {
			ctx.response().setStatusCode(404).putHeader("Content-Type", "application/json").end(Json.encodeToBuffer(new JsonObject().put("success", false).put("message", "camera not found")));
			return;
		}
		
		List<Recording> recordings = RtspRecorder.getInstance().getDatabase().getRecordings(date, cameraName);
		if(recordings == null) {
			ctx.response().setStatusCode(500);
			return;
		}
		
		JsonArray json = new JsonArray();
		recordings.forEach(recording -> json.add(recording.toJson()));
		ctx.json(json);
	}

}
