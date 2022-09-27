package com.melluh.rtsprecorder.http;

import com.grack.nanojson.JsonArray;
import com.melluh.rtsprecorder.Clip;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;
import com.melluh.simplehttpserver.router.Route;

import java.util.List;

public class ClipsRoute implements Route {

    @Override
    public Response serve(Request request) {
        List<Clip> clips = RtspRecorder.getInstance().getDatabase().getClips();

        JsonArray clipsArray = new JsonArray();
        clips.forEach(clip -> clipsArray.add(clip.toJson()));
        return WebServer.jsonResponse(Status.OK, clipsArray);
    }

}
