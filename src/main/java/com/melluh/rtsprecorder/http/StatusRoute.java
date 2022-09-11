package com.melluh.rtsprecorder.http;

import com.grack.nanojson.JsonObject;
import com.melluh.simplehttpserver.router.Route;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.CameraProcess;
import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FileUtil;
import com.melluh.rtsprecorder.util.FormatUtil;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;

public class StatusRoute implements Route {

	@Override
	public Response serve(Request request) {
		JsonObject camerasJson = new JsonObject();
		for (Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			CameraProcess process = camera.getProcess();
			camerasJson.put(camera.getName(), JsonObject.builder()
					.value("status", process.getStatus().name())
					.value("statusSince", process.getStatusSince())
					.value("pid", process.getPid())
					.value("fps", process.getFps())
					.value("failedStarts", process.getFailedStarts())
					.done());
		}

		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		long recordingsSize = FileUtil.getFolderSize(configHandler.getRecordingsFolder());
		long maxSize = configHandler.getRecordingsMaxSize();
		float percentage = Math.min(((float) recordingsSize / (float) maxSize) * 100.0f, 100);

		return WebServer.jsonResponse(Status.OK, JsonObject.builder()
				.object("diskUsage")
					.value("readable", FormatUtil.readableFileSize(recordingsSize))
					.value("percentage", String.format("%.1f", percentage) + "%")
					.end()
				.value("cameras", camerasJson)
				.done());
	}
	
}
