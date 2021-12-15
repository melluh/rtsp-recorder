package com.melluh.rtsprecorder.task;

import java.util.concurrent.TimeUnit;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.CameraProcess;
import com.melluh.rtsprecorder.RtspRecorder;
import com.melluh.rtsprecorder.CameraProcess.ProcessStatus;

public class WatchdogTask implements Runnable {
	
	@Override
	public void run() {
		long currentTime = System.currentTimeMillis();
		for(Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			CameraProcess process = camera.getProcess();
			if(process.getStatus() != ProcessStatus.WORKING)
				continue;
			
			long timePassed = currentTime - process.getLastUpdate();
			if(currentTime >= process.getRetryTime() && currentTime - process.getLastUpdate() > camera.getTimeout()) {
				if(process.getFailedStarts() >= 5) {
					process.setRetryTime(currentTime + TimeUnit.MINUTES.toMillis(2));
					RtspRecorder.LOGGER.warning(camera.getName() + " failed to start 5 times, retrying in 2 minutes.");
					continue;
				}
				
				RtspRecorder.LOGGER.warning(camera.getName() + " got stuck! Last update was " + timePassed + "ms ago (timeout=" + camera.getTimeout() + "ms), restarting it's FFmpeg process.");
				RtspRecorder.getInstance().getThreadPool().submit(process::restart);
			}
		}
	}
	
}
