package com.melluh.rtsprecorder.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
	
}
