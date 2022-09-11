package com.melluh.rtsprecorder.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {

	private FormatUtil() {}
	
	public static String readableFileSize(long size) {
	    if(size <= 0)
	    	return "0 B";
	    
	    final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
	    int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
	    
	    DecimalFormat df = new DecimalFormat("#.##");
	    df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US)); // Use dots instead of commas
	    return df.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
	public static float parseFloat(String str) {
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}
	
	public static int parseInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}
	
	public static LocalDate parseDate(String str) {
		if(str == null || str.isEmpty())
			return null;
		
		try {
			return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(str));
		} catch (DateTimeException ignored) {
			return null;
		}
	}
	
	public static LocalDateTime parseDateTime(String str) {
		if(str == null || str.isEmpty())
			return null;
		
		try {
			return LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(str));
		} catch (DateTimeException ignored) {
			return null;
		}
	}
	
	public static String formatDate(LocalDate date) {
		return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}
	
	public static String formatTime(LocalTime time) {
		return time.format(DateTimeFormatter.ofPattern("HH.mm.ss"));
	}
	
	public static String formatTimeTook(long millis) {
		float seconds = Math.max((float) millis / 1000.0f, 0.0f);
		return String.format("%.02f", seconds) + "s";
	}

	public static String formatDuration(Duration duration) {
		return String.format("%02d", duration.toHours()) + ":" + String.format("%02d", duration.toMinutesPart()) + ":" + String.format("%02d", duration.toSecondsPart());
	}
	
}
