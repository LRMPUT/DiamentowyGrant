package org.dg.camera;

import android.util.Log;

public class VisualCompass implements Runnable  {
	final String moduleLogName = "VisualCompass";
	
	Thread visualCompassThread;
	boolean visualCompassThreadRun = true;
	
	public void startThread() {
		visualCompassThread = new Thread(this, "Visual compass");
		visualCompassThreadRun = true;
		visualCompassThread.start();
	}
	
	public void stopThread() {
		try {
			visualCompassThreadRun = false;
			visualCompassThread.join();
		} catch (InterruptedException e) {
			Log.e(moduleLogName, "Failed to join Visual Compass thread");
		}
	}
	
	public boolean isThreadRunning() {
		return visualCompassThreadRun;
	}
	
	
	@Override
	public void run() {
		Log.d(moduleLogName, "Started Visual Compass thread");
		while ( visualCompassThreadRun ) {
			
			try {
				Log.d(moduleLogName, "Visual Compass thread -- idle ...");
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Log.e(moduleLogName, "Failed to sleep Visual Compass thread");
			}
			
		}
		Log.d(moduleLogName, "Finished Visual Compass thread");
	}

}
