package com.melluh.rtsprecorder.http;

import com.grack.nanojson.JsonObject;
import com.melluh.rtsprecorder.Clip;
import com.melluh.rtsprecorder.Recording;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FormatUtil;
import com.melluh.simplehttpserver.Request;
import com.melluh.simplehttpserver.protocol.Status;
import com.melluh.simplehttpserver.response.Response;
import com.melluh.simplehttpserver.router.Route;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClipsCreateRoute implements Route {

    @Override
    public Response serve(Request req) {
        String cameraName = req.getQueryParam("camera");
        if (cameraName == null || cameraName.isEmpty())
            return getErrorResponse(Status.BAD_REQUEST, "request is missing camera");

        String label = req.getQueryParam("label");
        if (label == null || label.isEmpty())
            return getErrorResponse(Status.BAD_REQUEST, "request is missing label");

        if(RtspRecorder.getInstance().getCameraRegistry().getCamera(cameraName) == null)
            return getErrorResponse(Status.NOT_FOUND, "camera not found");

        LocalDateTime from = FormatUtil.parseDateTime(req.getQueryParam("from"));
        if (from == null)
            return getErrorResponse(Status.BAD_REQUEST, "request is missing from time");

        LocalDateTime to = FormatUtil.parseDateTime(req.getQueryParam("to"));
        if (to == null)
            return getErrorResponse(Status.BAD_REQUEST, "request is missing to time");

        if (from.isAfter(to))
            return getErrorResponse(Status.BAD_REQUEST, "from must be before to");

        List<Recording> recordings = RtspRecorder.getInstance().getDatabase().getRecordings(from, to, cameraName);
        if (recordings == null)
            return getErrorResponse(Status.INTERNAL_SERVER_ERROR, "An error occurred");

        Recording firstRecording = recordings.get(0);
        Duration spliceStart = nonNegative(Duration.between(firstRecording.getStartTime(), from));
        Recording lastRecording = recordings.get(recordings.size() - 1);
        Duration spliceEndOffset = nonNegative(Duration.between(to, lastRecording.getEndTime()));

        Duration length = recordings.stream()
                .map(Recording::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
        Duration spliceEnd = length.minus(spliceEndOffset);

        String id = UUID.randomUUID().toString().replace("-", "");

        try {
            File directory = new File(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder(), "clips");
            Files.createDirectories(directory.toPath());

            String fileContents = recordings.stream()
                    .map(recording -> "file '../" + recording.getFilePath() + "'")
                    .collect(Collectors.joining("\n"));
            File tempFile = new File(directory, id + ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), fileContents);

            String command = "ffmpeg -hide_banner -ss %start% -to %end% -f concat -safe 0 -i %id%.txt -c copy %id%.mp4"
                    .replace("%id%", id)
                    .replace("%start%", FormatUtil.formatDuration(spliceStart))
                    .replace("%end%", FormatUtil.formatDuration(spliceEnd));

            Process process = new ProcessBuilder(command.split(" "))
                    .directory(directory)
                    .start();

            process.waitFor();
            tempFile.delete();

            long exitCode = process.exitValue();
            if (exitCode != 0) {
                return WebServer.jsonResponse(Status.INTERNAL_SERVER_ERROR, JsonObject.builder()
                        .value("success", false)
                        .value("msg", "FFmpeg exited with code " + exitCode)
                        .done());
            }

            String filePath = "clips/" + id + ".mp4";
            Clip clip = new Clip(filePath, label, cameraName, from, to, LocalDateTime.now());
            RtspRecorder.getInstance().getDatabase().storeClip(clip);

            return WebServer.jsonResponse(Status.OK, JsonObject.builder()
                    .value("success", true)
                    .value("result", clip.toJson())
                    .done());
        } catch (IOException | InterruptedException ex) {
            Logger.error(ex, "Failed to run FFmpeg");
            return getErrorResponse(Status.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private static Response getErrorResponse(Status status, String msg) {
        return WebServer.jsonResponse(status, JsonObject.builder()
                .value("success", false)
                .value("message", msg)
                .done());
    }

    private static Duration nonNegative(Duration duration) {
        if (duration.isNegative())
            return Duration.ZERO;
        return duration;
    }

}
