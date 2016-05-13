package org.dg.camera;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.os.Environment;
import android.util.Log;

public class VisualCompass implements Runnable  {
	final String moduleLogName = "VisualCompass";
	
	static {
		System.loadLibrary("VisualOdometryModule");
	}
	
	public native void NDKEstimateRotation(long matAddrImg1, long matAddrImg2);
	public native void WTF();
	
	Thread visualCompassThread;
	boolean visualCompassThreadRun = false;
	
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
		System.loadLibrary("VisualOdometryModule");
		
		testRun();
		Log.d(moduleLogName, "Finished test run!");
		
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
	
	private void testRun() {
		File root = Environment.getExternalStorageDirectory();
		File file1 = new File(root, "OpenAIL/VisualCompass/testImage1.png");
		File file2 = new File(root, "OpenAIL/VisualCompass/testImage2.png");
		
		// OpenCV 3.0.0: Mat img = Imgcodecs.imread(file.getAbsolutePath()), dst = new Mat();
		Mat img1 = Highgui.imread(file1.getAbsolutePath());
		Mat img2 = Highgui.imread(file2.getAbsolutePath());
		
		Log.d(moduleLogName, "Loaded images! Calling NDK");
		
		NDKEstimateRotation(img1.getNativeObjAddr(), img2.getNativeObjAddr());

	}

}
