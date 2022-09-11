package com.melluh.rtsprecorder.http;

import java.time.LocalDate;
import java.util.List;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.melluh.simplehttpserver.router.Route;

import com.melluh.rtsprecorder.Recording;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FormatUtil;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;

public class RecordingsRoute implements Route {

	@Override
	public Response serve(Request req) {
		LocalDate date = FormatUtil.parseDate(req.getQueryParam("date"));
		if (date == null)
			return getErrorResponse(Status.BAD_REQUEST, "request is missing date");
		
		String cameraName = req.getQueryParam("camera");
		if (cameraName == null || cameraName.isEmpty())
			return getErrorResponse(Status.BAD_REQUEST, "request is missing camera");
		
		if(RtspRecorder.getInstance().getCameraRegistry().getCamera(cameraName) == null)
			return getErrorResponse(Status.NOT_FOUND, "camera not found");
		
		List<Recording> recordings = RtspRecorder.getInstance().getDatabase().getRecordings(date, cameraName);
		if (recordings == null)
			return getErrorResponse(Status.INTERNAL_SERVER_ERROR, "An error occurred");

		JsonArray recordingsArray = new JsonArray();
		recordings.forEach(recording -> recordingsArray.add(recording.toJson()));
		return WebServer.jsonResponse(Status.OK, recordingsArray);
	}
	
	private static Response getErrorResponse(Status status, String msg) {
		return WebServer.jsonResponse(status, JsonObject.builder()
				.value("success", false)
				.value("message", msg)
				.done());
	}

}
