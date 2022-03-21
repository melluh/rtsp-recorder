package com.melluh.rtsprecorder.task;

import java.io.File;
import java.time.LocalDateTime;

import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.Recording;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FileUtil;
import com.melluh.rtsprecorder.util.FormatUtil;
import org.tinylog.Logger;

public class MoveRecordingsTask implements Runnable {

	@Override
	public void run() {
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		File recordingsDir = configHandler.getRecordingsFolder();
		File tempDir = configHandler.getTempRecordingsFolder();
		
		int deletedCount = 0;
		for(File file : tempDir.listFiles()) {
			String fileName = file.getName();
			if(!file.isFile() || !fileName.endsWith(".mp4") || RtspRecorder.getInstance().getCameraRegistry().isActiveFile(fileName))
				continue;
			
			int seperatorIndex = fileName.indexOf('-');
			if(seperatorIndex == -1) {
				Logger.warn("Invalid filename {}, deleting file.", fileName);
				this.delete(file);
				continue;
			}
			
			String cameraName = fileName.substring(0, seperatorIndex);
			String dateTimeStr = fileName.substring(seperatorIndex + 1, fileName.lastIndexOf('.'));
			
			if(RtspRecorder.getInstance().getCameraRegistry().getCamera(cameraName) == null) {
				Logger.warn("Unknown camera '{}', deleting file.", cameraName);
				this.delete(file);
				continue;
			}
			
			LocalDateTime startTime = FileUtil.parseFileDateTime(dateTimeStr);
			if(startTime == null) {
				Logger.warn("Invalid filename {}, deleting file.", fileName);
				this.delete(file);
				continue;
			}
			
			float duration = FileUtil.getRecordingDuration(file);
			if(duration <= 0) {
				deletedCount++;
				this.delete(file);
				continue;
			}
			
			LocalDateTime endTime = startTime.plusSeconds((long) duration);	
			String path = FormatUtil.formatDate(startTime.toLocalDate()) + "/" + cameraName + "/" + FormatUtil.formatTime(startTime.toLocalTime()) + ".mp4";
			
			RtspRecorder.getInstance().getDatabase().storeRecording(new Recording(path, cameraName, startTime, endTime));
			
			File newFile = new File(recordingsDir, path);
			newFile.getParentFile().mkdirs();
			
			if(!file.renameTo(newFile)) {
				Logger.warn("Failed to move {} to {}", file.getAbsolutePath(), newFile.getAbsolutePath());
			}
		}
		
		if(deletedCount > 0) {
			Logger.info("Deleted {} corrupt/incomplete recording(s)", deletedCount);
		}
	}
	
	private void delete(File file) {
		if(!file.delete())
			Logger.warn("Failed to delete {}", file.getName());
	}

}
