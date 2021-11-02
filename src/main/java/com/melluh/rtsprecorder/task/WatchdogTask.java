package com.melluh.rtsprecorder.task;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.RtspRecorder;

public class WatchdogTask implements Runnable {
	
	@Override
	public void run() {
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
