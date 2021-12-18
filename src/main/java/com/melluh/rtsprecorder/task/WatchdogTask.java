package com.melluh.rtsprecorder.task;

import com.melluh.rtsprecorder.Camera;
import com.melluh.rtsprecorder.CameraProcess;
import com.melluh.rtsprecorder.CameraProcess.ProcessStatus;
import com.melluh.rtsprecorder.RtspRecorder;

public class WatchdogTask implements Runnable {
	
	@Override
	public void run() {
		long currentTime = System.currentTimeMillis();
		for(Camera camera : RtspRecorder.getInstance().getCameraRegistry().getCameras()) {
			CameraProcess process = camera.getProcess();
			
			if(process.getStatus() == ProcessStatus.STARTING) {
				long timePassed = currentTime - process.getStatusSince();
				if(currentTime >= process.getRetryTime() && timePassed > camera.getStartTimeout()) {
					process.handleTimeout(timePassed);
				}
				
				continue;
			}
			
			if(process.getStatus() == ProcessStatus.WORKING) {
				long timePassed = currentTime - process.getLastUpdate();
				if(timePassed > camera.getTimeout()) {
					process.handleTimeout(timePassed);
				}
				
				continue;
			}
		}
	}
	
}
