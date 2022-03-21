package com.melluh.rtsprecorder.task;

import java.io.File;
import java.util.List;

import com.melluh.rtsprecorder.ConfigHandler;
import com.melluh.rtsprecorder.Database;
import com.melluh.rtsprecorder.Recording;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.util.FileUtil;
import com.melluh.rtsprecorder.util.FormatUtil;
import org.tinylog.Logger;

public class CleanupRecordingsTask implements Runnable {

	@Override
	public void run() {
		ConfigHandler configHandler = RtspRecorder.getInstance().getConfigHandler();
		long maxSize = configHandler.getRecordingsMaxSize();
		if(maxSize <= 0)
			return;
		
		long folderSize = FileUtil.getFolderSize(configHandler.getRecordingsFolder());
		if(maxSize > folderSize)
			return;
		
		long targetSize = Math.min(maxSize - configHandler.getRecordingsMaxSizeMargin(), maxSize);
		if(targetSize < 0)
			targetSize = 0;

		Logger.info("Recordings folder is too large, cleaning up recordings ({}/{}) - target is {}", FormatUtil.readableFileSize(folderSize), FormatUtil.readableFileSize(maxSize), FormatUtil.readableFileSize(targetSize));
		int numRemoved = 0;
		
		Database database = RtspRecorder.getInstance().getDatabase();
		while(targetSize <= folderSize) {
			List<Recording> recordings = database.getOldestRecordings(10);
			if(recordings.isEmpty())
				break;
			
			for(Recording recording : recordings) {
				File file = recording.getFile();
				if(!file.exists()) {
					Logger.warn("Invalid recording '{}', file doesn't exist - removing from  database");
					database.removeRecording(recording.getFilePath());
					continue;
				}
				
				long fileSize = file.length();
				
				if(!file.delete()) {
					Logger.warn("Failed to delete recording '{}'", recording.getFilePath());
					continue;
				}
				
				database.removeRecording(recording.getFilePath());
				numRemoved++;
				
				folderSize -= fileSize;
				if(targetSize > folderSize)
					break;
			}
		}

		Logger.info("{} files deleted, folder size now {}", numRemoved, FormatUtil.readableFileSize(folderSize));
	}

}
