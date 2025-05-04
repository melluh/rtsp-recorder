package com.melluh.rtsprecorder.task;

import java.io.File;
import java.time.LocalDateTime;

import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.FileHandler;
import com.melluh.rtsprecorder.RtspRecorder;
import org.tinylog.Logger;

public class MoveRecordingsTask implements Runnable {

	@Override
	public void run() {
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		File recordingsDir = configHandler.getRecordingsFolder();
		File tempDir = configHandler.getTempRecordingsFolder();
		
		int deletedCount = 0;
		for (File file : tempDir.listFiles()) {
			String fileName = file.getName();
			if (!file.isFile() || !fileName.endsWith(".mp4") || RtspRecorder.getInstance().getCameraRegistry().isActiveFile(fileName))
				continue;

			try {
				FileHandler.process(file);
			} catch (Exception ex) {
				Logger.error(ex, "Failed to process file {} - deleting.", fileName);
				this.delete(file);
				deletedCount++;
			}
		}
		
		if (deletedCount > 0) {
			Logger.info("Deleted {} corrupt/incomplete recording(s)", deletedCount);
		}
	}

	public record FileInfo(String cameraName, LocalDateTime startTime, float duration) {}
	
	private void delete(File file) {
		if (!file.delete())
			Logger.warn("Failed to delete {}", file.getName());
	}

}
