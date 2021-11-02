package com.melluh.rtsprecorder.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.melluh.rtsprecorder.RtspRecorder;

public class FileUtil {

	private FileUtil() {}
	
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss");
	
	public static LocalDateTime parseFileDateTime(String fileName) {
		try {
			return LocalDateTime.from(FORMATTER.parse(fileName));
		} catch (DateTimeException ex) {
			RtspRecorder.LOGGER.warning("Failed to parse " + fileName);
			return null;
		}
	}
	
	public static float getRecordingDuration(File file) {
		try {
			Process process = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", file.getName())
					.directory(file.getParentFile())
					.start();
			process.waitFor();
			
			if(process.exitValue() != 0) {
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
