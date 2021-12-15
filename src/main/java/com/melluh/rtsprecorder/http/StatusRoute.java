package com.melluh.rtsprecorder.http;

import org.json.JSONObject;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.CameraProcess;
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
			JSONObject cameraJson = new JSONObject();
			cameraJson.put("status", process.getStatus());
			cameraJson.put("statusSince", process.getStatusSince());
			cameraJson.put("pid", process.getPid());
			cameraJson.put("fps", process.getFps());
			cameraJson.put("failedStarts", process.getFailedStarts());
			camerasJson.put(camera.getName(), cameraJson);
		}
		
		JSONObject json = new JSONObject()
				.put("diskUsage", FormatUtil.readableFileSize(FileUtil.getFolderSize(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder())))
				.put("cameras", camerasJson);
		
		return new Response(Status.OK)
				.contentType(MimeType.JSON)
				.body(json.toString());
	}
	
}
