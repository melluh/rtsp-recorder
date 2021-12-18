package com.melluh.rtsprecorder.http;

import org.json.JSONObject;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.CameraProcess;
import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FileUtil;
import com.melluh.rtsprecorder.util.FormatUtil;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.MimeType;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;

public class StatusRoute {

	public static Response handle(Request req) {
		JSONObject camerasJson = new JSONObject();
		for(Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			CameraProcess process = camera.getProcess();
			JSONObject cameraJson = new JSONObject()
					.put("status", process.getStatus())
					.put("statusSince", process.getStatusSince())
					.put("pid", process.getPid())
					.put("fps", process.getFps())
					.put("failedStarts", process.getFailedStarts());
			camerasJson.put(camera.getName(), cameraJson);
		}
		
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		long recordingsSize = FileUtil.getFolderSize(configHandler.getRecordingsFolder());
		long maxSize = configHandler.getRecordingsMaxSize();
		float percentage = Math.min(((float) recordingsSize / (float) maxSize) * 100.0f, 100);
		
		JSONObject json = new JSONObject()
				.put("diskUsage", new JSONObject()
						.put("readable", FormatUtil.readableFileSize(recordingsSize))
						.put("percentage", String.format("%.1f", percentage) + "%"))
				.put("cameras", camerasJson);
		
		return new Response(Status.OK)
				.contentType(MimeType.JSON)
				.body(json.toString());
	}
	
}
