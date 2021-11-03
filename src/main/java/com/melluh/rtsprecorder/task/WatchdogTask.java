package com.melluh.rtsprecorder.task;

import java.util.concurrent.TimeUnit;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.RtspRecorder;

public class WatchdogTask implements Runnable {
	
	@Override
	public void run() {
		long currentTime = System.currentTimeMillis();
		for(Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			long timePassed = currentTime - camera.getLastUpdate();
			if(!camera.isRestarting() && timePassed >= camera.getTimeout() && currentTime >= camera.getRetryStartTime()) {
				if(camera.getFailedStarts() >= 5) {
					camera.setRetryStartTime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2));
					RtspRecorder.LOGGER.warning(camera.getName() + " failed to start 5 times, retrying in 2 minutes.");
					continue;
				}
				
				RtspRecorder.LOGGER.warning(camera.getName() + " got stuck! Last update was " + timePassed + "ms ago (timeout=" + camera.getTimeout() + "ms), restarting it's FFmpeg process.");
				RtspRecorder.getInstance().getThreadPool().submit(camera::restartProcess);
			}
		}
	}
	
}
