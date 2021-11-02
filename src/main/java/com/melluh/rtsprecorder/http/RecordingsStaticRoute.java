package com.melluh.rtsprecorder.http;

import java.io.File;
import java.util.Objects;

import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.RtspRecorder;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RecordingsStaticRoute implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext ctx) {
		String path = ctx.normalizedPath();
		if(!path.startsWith("/recordings/")) {
			ctx.response().setStatusCode(400).send();
			return;
		}
		path = path.substring("/recordings/".length());
		
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		File file = new File(configHandler.getRecordingsFolder(), path);
		
		if(file.getParentFile().equals(configHandler.getTempRecordingsFolder()) || !file.getName().endsWith(".mp4")) {
			ctx.response().setStatusCode(403).send();
			return;
		}
		
		if(!file.exists()) {
			ctx.next();
			return;
		}
		
		if(Objects.equals(ctx.queryParams().get("download"), "1")) {
			ctx.response().putHeader("Content-Type", "application/octet-stream");
		}
		
		ctx.response().sendFile(file.getAbsolutePath());
	}

}
