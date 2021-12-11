package com.melluh.rtsprecorder.http;

import java.time.LocalDate;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.melluh.rtsprecorder.Recording;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FormatUtil;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.MimeType;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;

public class RecordingsRoute {

	public static Response handle(Request req) {
		LocalDate date = FormatUtil.parseDate(req.getQueryParam("date"));
		if(date == null) {
			return getErrorResponse(Status.BAD_REQUEST, "request is missing date");
		}
		
		String cameraName = req.getQueryParam("camera");
		if(cameraName == null || cameraName.isEmpty()) {
			return getErrorResponse(Status.BAD_REQUEST, "request is missing camera");
		}
		
		if(RtspRecorder.getInstance().getCameraRegistry().getCamera(cameraName) == null) {
			return getErrorResponse(Status.NOT_FOUND, "camera not found");
		}
		
		List<Recording> recordings = RtspRecorder.getInstance().getDatabase().getRecordings(date, cameraName);
		if(recordings == null) {
			return getErrorResponse(Status.INTERNAL_SERVER_ERROR, "An error occurred");
		}
		
		JSONArray json = new JSONArray();
		recordings.forEach(recording -> json.put(recording.toJson()));
		
		return new Response(Status.OK)
				.contentType(MimeType.JSON)
				.body(json.toString());
	}
	
	private static Response getErrorResponse(Status status, String msg) {
		return new Response(status)
				.contentType(MimeType.JSON)
				.body(new JSONObject().put("success", false).put("message", "request is missing date").toString());
	}

}
