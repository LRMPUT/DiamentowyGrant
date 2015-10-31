package org.dg.camera;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;

public class VisualPlaceRecognition implements Runnable {
	
	private static final String moduleLogName = "VisualPlaceRecognitionJava";
	
	static {
		try {
		System.loadLibrary("VisualPlaceRecognitionModule");
		} catch (UnsatisfiedLinkError e)
		{
			Log.d(moduleLogName, "VPR Native code library failed to load.\n" + e);
		}
	}
	
	// Native methods
	private native long createAndLoadFabmapNDK(String settingsPath);
	private native long createAndTrainFabmapNDK(String settingsPath, int trainingSetSize);
	private native void addTestSetFabmapNDK(long addrFabmapEnv, int testSetSize);
	private native int testLocationFabmapNDK(long addrFabmapEnv, long addrImageToTest, boolean addToTest);
	private native void destroyFabmapNDK(long addrFabmap);
	
	/// Java code	
	
	// Fabmap object address
	long addrFabMapEnv;
	
	// Training images
	Mat[] trainImages;
	
	// Test images
	Mat[] testImages;
	
	// Processing thread
	Thread vprThread;
	boolean performVPR;
	
	// Preview to get current image
	Preview preview;
	
	// Add images to test
	ReentrantLock placesToAddLock = new ReentrantLock();
	ReentrantLock matchedListLock = new ReentrantLock();
	
	List<Mat> placesToAdd;
	List<Integer> fabmapIDs;
	
	List<org.dg.openAIL.IdPair<Integer,Integer>> vprMatchedList;
	
	int saveImageNextNum = 0;
	
	public VisualPlaceRecognition() {
		
		//Initialize FabMapEnv pointer to NULL
		addrFabMapEnv = 0;
	}

	public void start(Preview _preview) {
		
		placesToAdd = new ArrayList<Mat>();
		fabmapIDs = new ArrayList<Integer>();
		vprMatchedList = new ArrayList<org.dg.openAIL.IdPair<Integer, Integer>>();
		
		preview = _preview;
		performVPR = true;
		vprThread = new Thread(this, "VPR thread");
		vprThread.start();
	}
	
	public void stop() {
		performVPR = false;
		try {
			vprThread.join();
		} catch (InterruptedException e) {
			Log.d(moduleLogName, "Failed to join recognizePlacesThread");
		}
	}

	// TODO: Just for test
	public void callAndVerifyAllMethods() {
		trainVisualPlaceRecognition();
		
		{
			try{
				String resPath = String.format(Locale.getDefault(), Environment
						.getExternalStorageDirectory().toString()
						+ "/OpenAIL"
						+ "/VPR/res.log");
				PrintStream resStream = new PrintStream(new FileOutputStream(resPath));
				
				String fabmapTestRecPath = String.format(Locale.getDefault(), Environment
						.getExternalStorageDirectory().toString()
						+ "/OpenAIL"
						+ "/VPR/testRec/");
				
				File fTestRec = new File(fabmapTestRecPath);        
				File fileTestRec[] = fTestRec.listFiles();
				Log.d(moduleLogName, "Test rec size: " + fileTestRec.length);
				
				for (int i=0; i < fileTestRec.length; i++)
				{
				    Log.d(moduleLogName, "Test rec filename:" + fileTestRec[i].getName());
				    Mat testRecImage = Highgui.imread(fabmapTestRecPath + fileTestRec[i].getName(), 0);
				    
				    int match = recognizePlace(testRecImage);
				    
				    resStream.print(String.format("%d %d\n", i + 1, match + 1));
				}
				
				resStream.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		destroyFabmapNDK(addrFabMapEnv);
	}
	
	// Method used to run the training of FABMAP
	public void trainVisualPlaceRecognition() {
		Log.d(moduleLogName, "Called trainVisualPlaceRecognition()");
		
		if(addrFabMapEnv != 0){
			destroyFabmapNDK(addrFabMapEnv);
			addrFabMapEnv = 0;
		}
		
		// Read training images
		int trainingSetSize;
		{
			String fabmapTrainPath = String.format(Locale.getDefault(), Environment
					.getExternalStorageDirectory().toString()
					+ "/OpenAIL"
					+ "/VPR/train/");
			
			File fTrain = new File(fabmapTrainPath);        
			File fileTrain[] = fTrain.listFiles();
			Log.d(moduleLogName, "Train size: " + fileTrain.length);
			trainImages = new Mat[fileTrain.length];
			
			for (int i=0; i < fileTrain.length; i++)
			{
			    Log.d(moduleLogName, "Train filename:" + fileTrain[i].getName());
			    trainImages[i] = Highgui.imread(fabmapTrainPath + fileTrain[i].getName(), 0);
			}
			trainingSetSize = fileTrain.length;
		}

		Log.d(moduleLogName, "training FabMap");
		// Fabmap settings path
		{
			createAndLoadFabmap();
		}
		
		// Read testimages
		int testSetSize;

		String fabmapTestPath = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL"
				+ "/VPR/test/");

		File fTest = new File(fabmapTestPath);
		File fileTest[] = fTest.listFiles();
		Log.d(moduleLogName, "Test size: " + fileTest.length);
		testImages = new Mat[fileTest.length];

		for (int i = 0; i < fileTest.length; i++) {
			Log.d(moduleLogName, "Test filename:" + fileTest[i].getName());
			testImages[i] = Highgui.imread(
					fabmapTestPath + fileTest[i].getName(), 0);
		}
		testSetSize = fileTest.length;

		Log.d(moduleLogName, "adding test images");
		addTestSetFabmapNDK(addrFabMapEnv, testSetSize);
	}
	/**
	 * 
	 */
	private void createAndLoadFabmap() {
		String fabmapSettingsPath = String.format(Locale.getDefault(), Environment
		.getExternalStorageDirectory().toString()
		+ "/OpenAIL"
		+ "/VPR/settings.yml");
		
		// Call training of fabmap library
		//addrFabMapEnv = createAndTrainFabmapNDK(fabmapSettingsPath, trainingSetSize);
		addrFabMapEnv = createAndLoadFabmapNDK(fabmapSettingsPath);
	}
	
	// Method used to try to match new image from camera to existing dataset of images
	public int recognizePlace(Mat imageToTest) {
		int match = testLocationFabmapNDK(addrFabMapEnv, imageToTest.getNativeObjAddr(), false);

		Log.d(moduleLogName, "Recognized place: " + match);
		
		return match;
	}
	
	public void savePlace(double x, double y, double z, Mat image){
		
		savePlace(x, y, z, image, saveImageNextNum);
				
		saveImageNextNum++;
	}
	
	public void savePlace(double x, double y, double z, Mat image, int id){
		
		String path = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL"
				+ "/VPR"
				+ "/images"
				+ "/rec/");
		
		String imagePath = path + String.format("img%04d.png", id);
		
		Log.d(moduleLogName, "Saving image to: " + imagePath);
		Log.d(moduleLogName, String.format("image size = (%d, %d)", image.cols(), image.rows()));
		
		Highgui.imwrite(imagePath, image);
		
		
	}
	
	
	public void addPlace(int id, Mat image) {
		placesToAddLock.lock();
		fabmapIDs.add(id);
		placesToAdd.add(image);
		placesToAddLock.unlock();
	}
	
	@Override
	public void run() {
		
		// Load train database (train has to been perform before)
		createAndLoadFabmap();
		
		// Load test images
		
		while(performVPR) {
			// Get new image
			Mat img = preview.getCurPreviewImage();
			
			// Try to recognize that image
			int fabmapID = recognizePlace(img);
			
			// Add to a list of possible matches
			if (fabmapID > 0) {
				matchedListLock.lock();
				// TODO: Jakie jest current ID?
				
				matchedListLock.unlock();
			}			
			
			// Add images to test (if any)
			placesToAddLock.lock();
			while(placesToAdd.size() != 0)
			{
				// Add place to recognition in NDK
				// ....
				
				placesToAdd.remove(0);
			}
			placesToAddLock.unlock();
		}
		
	}

	
	public List<org.dg.openAIL.IdPair<Integer, Integer>> getAndClearVPRMatchedList() {
		
		try {
			matchedListLock.lock();
			List<org.dg.openAIL.IdPair<Integer, Integer>> returnList = new ArrayList<org.dg.openAIL.IdPair<Integer, Integer>>(vprMatchedList.size());
			for(org.dg.openAIL.IdPair<Integer, Integer> item: vprMatchedList) 
				returnList.add( new org.dg.openAIL.IdPair<Integer, Integer>(item) );
			vprMatchedList.clear();
			
			matchedListLock.unlock();
			return returnList;
		} catch (Exception e) {
			Log.d(moduleLogName, "Failed to acquire recognizedList Mtx in getRecognizedPlaces");
		}
		return new ArrayList<org.dg.openAIL.IdPair<Integer, Integer>>();
		
		
		
		
	}
}
