package com.melluh.rtsprecorder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Watchdog {
	
	public void start() {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(this::runChecks, 1, 1, TimeUnit.SECONDS);
	}
	
	private void runChecks() {
		long currentTime = System.currentTimeMillis();
		for(Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			long timePassed = currentTime - camera.getLastUpdate();
			if(timePassed >= camera.getTimeout()) {
				RtspRecorder.LOGGER.warning(camera.getName() + " got stuck! Last update was " + timePassed + "ms ago (timeout=" + camera.getTimeout() + "ms), restarting it's FFmpeg process.");
				camera.restartProcess();
			}
		}
	}
	
}
