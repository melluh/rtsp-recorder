package com.melluh.rtsprecorder;

import com.google.common.base.Preconditions;
import com.melluh.rtsprecorder.task.MoveRecordingsTask.FileInfo;
import com.melluh.rtsprecorder.util.FileUtil;
import com.melluh.rtsprecorder.util.FormatUtil;
import org.tinylog.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileHandler {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss");

    public static synchronized void process(File file) { // Synchronized to avoid race condition issues
        if (!file.exists() || !file.isFile())
            return;

        FileInfo info = getFileInfo(file);
        Preconditions.checkState(RtspRecorder.getInstance().getCameraRegistry().getCamera(info.cameraName()) != null,
                "Unknown camera " + info.cameraName());

        LocalDateTime endTime = info.startTime().plusSeconds((long) info.duration());

        String path = FormatUtil.formatDate(info.startTime().toLocalDate()) + "/" + info.cameraName() + "/" + FormatUtil.formatTime(info.startTime().toLocalTime()) + ".mp4";
        RtspRecorder.getInstance().getDatabase().storeRecording(new Recording(path, info.cameraName(), info.startTime(), endTime));

        File newFile = new File(RtspRecorder.getInstance().getConfigHandler().getRecordingsFolder(), path);
        newFile.getParentFile().mkdirs();

        if (!file.renameTo(newFile)) {
            Logger.warn("Failed to move {} to {}", file.getAbsolutePath(), newFile.getAbsolutePath());
        }
    }

    private static FileInfo getFileInfo(File file) {
        Preconditions.checkState(file.isFile(), "File is not a file");
        Preconditions.checkState(file.getName().endsWith(".mp4"), "File must be .mp4");

        int separatorIndex = file.getName().indexOf('-');
        Preconditions.checkState(separatorIndex != -1, "Invalid filename " + file.getName());

        String cameraName = file.getName().substring(0, separatorIndex);
        String dateTimeStr = file.getName().substring(separatorIndex + 1, file.getName().lastIndexOf('.'));

        LocalDateTime startTime = LocalDateTime.from(DATE_TIME_FORMAT.parse(dateTimeStr));
        float duration = FileUtil.getRecordingDuration(file);

        return new FileInfo(cameraName, startTime, duration);
    }

}
