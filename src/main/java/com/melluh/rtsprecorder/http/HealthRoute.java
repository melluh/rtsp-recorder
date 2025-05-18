package com.melluh.rtsprecorder.http;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.CameraProcess.ProcessStatus;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;
import com.melluh.simplehttpserver.router.Route;

public class HealthRoute implements Route {

    @Override
    public Response serve(Request request) {
        String cameraName = request.getQueryParam("camera");
        if (cameraName == null || cameraName.isEmpty())
            return new Response(Status.BAD_REQUEST).body("Request is missing camera");

        Camera camera = RtspRecorder.getInstance().getCameraRegistry().getCamera(cameraName);
        if (camera == null)
            return new Response(Status.NOT_FOUND).body("Unknown camera name");

        ProcessStatus status = camera.getProcess().getStatus();
        return new Response(status == ProcessStatus.WORKING ? Status.OK : Status.INTERNAL_SERVER_ERROR)
                .body(status.name());
    }

}
