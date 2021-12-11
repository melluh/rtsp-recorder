package com.melluh.rtsprecorder.http;

import java.io.File;

import org.json.JSONObject;

import com.melluh.rtsprecorder.Camera;
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
			JSONObject cameraJson = new JSONObject();
			cameraJson.put("fps", camera.getFps());
			cameraJson.put("speed", camera.getSpeed());
			cameraJson.put("isWorking", camera.isWorking());
			cameraJson.put("connectedSince", camera.getConnectedSince());
			cameraJson.put("pid", camera.getPid());
			camerasJson.put(camera.getName(), cameraJson);
		}
		
		JSONObject json = new JSONObject()
				.put("diskUsage", FormatUtil.readableFileSize(FileUtil.getFolderSize(new File("recordings"))))
				.put("cameras", camerasJson);
		
		return new Response(Status.OK)
				.contentType(MimeType.JSON)
				.body(json.toString());
	}
	
}
