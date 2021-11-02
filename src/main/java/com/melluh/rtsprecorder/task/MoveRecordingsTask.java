package com.melluh.rtsprecorder.task;

import java.io.File;
import java.time.LocalDateTime;

import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.Recording;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FileUtil;
import com.melluh.rtsprecorder.util.FormatUtil;

public class MoveRecordingsTask implements Runnable {

	@Override
	public void run() {
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		File recordingsDir = configHandler.getRecordingsFolder();
		File tempDir = configHandler.getTempRecordingsFolder();
		
		int numMovedFiles = 0;
		for(File file : tempDir.listFiles()) {
			String fileName = file.getName();
			if(!file.isFile() || !fileName.endsWith(".mp4") || RtspRecorder.getInstance().getCameraRegistry().isInProgressFile(fileName))
				continue;
			
			int seperatorIndex = fileName.indexOf('-');
			if(seperatorIndex == -1) {
				RtspRecorder.LOGGER.warning("Invalid filename " + fileName + ", deleting file.");
				this.delete(file);
				continue;
			}
			
			String cameraName = fileName.substring(0, seperatorIndex);
			String dateTimeStr = fileName.substring(seperatorIndex + 1, fileName.lastIndexOf('.'));
			
			if(RtspRecorder.getInstance().getCameraRegistry().getCamera(cameraName) == null) {
				RtspRecorder.LOGGER.warning("Unknown camera '" + cameraName + "', deleting file.");
				this.delete(file);
				continue;
			}
			
			LocalDateTime startTime = FileUtil.parseFileDateTime(dateTimeStr);
			if(startTime == null) {
				RtspRecorder.LOGGER.warning("Invalid filename " + fileName + ", deleting file.");
				this.delete(file);
				continue;
			}
			
			float duration = FileUtil.getRecordingDuration(file);
			if(duration <= 0) {
				RtspRecorder.LOGGER.warning("Failed to determine recording duration for " + fileName + " (probably corrupt), deleting file.");
				this.delete(file);
				continue;
			}
			
			LocalDateTime endTime = startTime.plusSeconds((long) duration);	
			String path = FormatUtil.formatDate(startTime.toLocalDate()) + "/" + cameraName + "/" + FormatUtil.formatTime(startTime.toLocalTime()) + ".mp4";
			
			RtspRecorder.getInstance().getDatabase().storeRecording(new Recording(path, cameraName, startTime, endTime));
			
			File newFile = new File(recordingsDir, path);
			newFile.getParentFile().mkdirs();
			
			if(!file.renameTo(newFile)) {
				RtspRecorder.LOGGER.warning("Failed to move " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
			} else numMovedFiles++;
		}
		
		if(numMovedFiles > 0)
			RtspRecorder.LOGGER.info("Moved " + numMovedFiles + " recordings");
	}
	
	private void delete(File file) {
		if(!file.delete())
			RtspRecorder.LOGGER.warning("Failed to delete " + file.getName());
	}

}
