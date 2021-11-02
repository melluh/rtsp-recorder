package com.melluh.rtsprecorder.http;

import java.io.File;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FileUtil;
import com.melluh.rtsprecorder.util.FormatUtil;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class StatusRoute implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext ctx) {
		JsonObject json = new JsonObject();
		json.put("diskUsage", FormatUtil.readableFileSize(FileUtil.getFolderSize(new File("recordings"))));
		
		JsonObject camerasJson = new JsonObject();
		json.put("cameras", camerasJson);
		for(Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			JsonObject cameraJson = new JsonObject();
			cameraJson.put("fps", camera.getFps());
			cameraJson.put("speed", camera.getSpeed());
			cameraJson.put("isWorking", camera.isWorking());
			cameraJson.put("connectedSince", camera.getConnectedSince());
			cameraJson.put("pid", camera.getPid());
			camerasJson.put(camera.getName(), cameraJson);
		}
		
		ctx.json(json);
	}
	
}
