package org.dg.camera;
import java.util.Locale;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Environment;
import android.util.Log;

public class VisualPlaceRecognition {
	
	private static final String moduleLogName = "VisualPlaceRecognitionJava";
	
	static {
		try {
		System.loadLibrary("VisualPlaceRecognitionModule");
		} catch (UnsatisfiedLinkError e)
		{
			Log.d(moduleLogName, "Native code library failed to load.\n" + e);
		}
	}
	
	// Native methods
	private native long createFabmapNDK(String settingsPath);
	private native void trainNDK(long addrFabmap, int trainingSetSize);
	private native int testLocationNDK(long addrFabmap, long addrImageToTest);
	private native int destroyFabmapNDK(long addrFabmap);
	
	/// Java code	
	
	// Fabmap object address
	long addrFabmap;
	
	// Training images
	Mat[] trainingImages;
	
	public VisualPlaceRecognition() {
		
		// Fabmap settings path
		// MN: The format of the configuration file is up to You
		String fabmapSettingsPath = String.format(Locale.getDefault(), Environment
		.getExternalStorageDirectory().toString()
		+ "/OpenAIL"
		+ "/VPR/settings.cfg");
		
		// Create Fabmap object
		addrFabmap = createFabmapNDK(fabmapSettingsPath);
	}
	
	// TODO: Just for test
	public void callAndVerifyAllMethods() {
		trainVisualPlaceRecognition();
		recognizePlace(new Mat());
		destroyFabmapNDK(addrFabmap);
	}
	
	// Method used to run the training of FABMAP
	public void trainVisualPlaceRecognition() {
		Log.d(moduleLogName, "Called trainVisualPlaceRecognition()");
		
		// Read training images
		int trainingSetSize = 20;
		trainingImages = new Mat[trainingSetSize];
		for (int i=0;i<trainingSetSize;i++)
		{
			trainingImages[i] = new Mat();
		}
		
		// Call training of fabmap library
		trainNDK(addrFabmap, trainingSetSize);	
	}
	
	// Method used to try to match new image from camera to existing dataset of images
	public void recognizePlace(Mat imageToTest) {
		testLocationNDK(addrFabmap, imageToTest.getNativeObjAddr());
	}

}
