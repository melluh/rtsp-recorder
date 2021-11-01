package com.melluh.rtsprecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileUtil {

	private FileUtil() {}
	
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss");
	
	public static long getRecordingStart(String fileName) {
		int dotIndex = fileName.lastIndexOf(".");
		if(dotIndex == -1)
			return -1;
		
		String baseName = fileName.substring(0, dotIndex);
		
		try {
			LocalDateTime dateTime = LocalDateTime.from(FORMATTER.parse(baseName));
			return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} catch (DateTimeException ex) {
			RtspRecorder.LOGGER.warning("Failed to parse " + fileName);
			return -1;
		}
	}
	
	public static float getRecordingDuration(File file) {
		try {
			Process process = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", file.getName())
					.directory(file.getParentFile())
					.start();
			process.waitFor();
			
			if(process.exitValue() != 0) {
				RtspRecorder.LOGGER.warning(file.getParentFile().getName() + "/" + file.getName() + " is probably corrupt (ffprobe failed with exit code " + process.exitValue() + ")");
				return -1;
			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			
			return line != null ? Float.parseFloat(line) : -1;
		} catch (IOException | InterruptedException e) {
			RtspRecorder.LOGGER.severe("Failed to determine recording duration for " + file.getName());
			e.printStackTrace();
			return -1;
		}
	}
	
}