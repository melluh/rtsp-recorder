package com.melluh.rtsprecorder.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DateTimeException;
import java.time.LocalDateTime;

import com.melluh.rtsprecorder.task.MoveRecordingsTask;
import org.tinylog.Logger;

public class FileUtil {

	private FileUtil() {}
	
	public static float getRecordingDuration(File file) {
		try {
			Process process = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", file.getName())
					.directory(file.getParentFile())
					.start();
			process.waitFor();
			
			if (process.exitValue() != 0) {
				throw new RuntimeException("Failed to determine recording duration, ffprobe exit code was " + process.exitValue());
			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			return line != null ? Float.parseFloat(line) : -1;
		} catch (IOException | InterruptedException ex) {
			throw new RuntimeException("Failed to determine recording duration", ex);
		}
	}
	
	public static long getFolderSize(File directory) {
		if(!directory.isDirectory())
			return directory.length();
		
		long size = 0;
		for(File file : directory.listFiles()) {
			if(file.isDirectory()) {
				size += getFolderSize(file);
				continue;
			}
			size += file.length();
		}
		
		return size;
	}
	
}
