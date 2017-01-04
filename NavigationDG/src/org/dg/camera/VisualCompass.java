package org.dg.camera;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.os.Environment;
import android.util.Log;


import java.util.ArrayList;
import java.util.Locale;

public class VisualCompass implements Runnable  {
	final String moduleLogName = "VisualCompass";
	String TAG = "FastABLE";
	
	static {
		System.loadLibrary("VisualOdometryModule");
	}
	
	public native void NDKEstimateRotation(long matAddrImg1, long matAddrImg2);

	public native float[] NDKTestFASTABLE(String configFile,
			String[] trainImgsArray, int[] trainLengths,
			String[] testImgsArray, int testLength);
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
		testFastABLE();
		//testRun();
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
	
	private void testFastABLE() {
		// Config file
        String configFile = String.format(Locale.getDefault(), Environment
                .getExternalStorageDirectory().toString()
                + "/FastABLE"
                + "/config.txt");

        // Train data
        String[] trainImagesFiles;
        String[] testImagesFiles;

        // Lengths of segments
        int[] trainLengths;
        {
            String trainPath = String.format(Locale.getDefault(), Environment
                    .getExternalStorageDirectory().toString()
                    + "/FastABLE"
                    + "/train/");

            // Get segment dirs inside training dir
            File trainDir = new File(trainPath);
            File trainDirsArr[] = trainDir.listFiles();
            Log.d(TAG, "Train segments: " + trainDirsArr.length);

            // Read segment dirs and put into ArrayList
            ArrayList<String> trainImgsList = new ArrayList<String>();
            ArrayList<Integer> trainLengthsList = new ArrayList<Integer>();
            for (int d = 0; d < trainDirsArr.length; d++) {
                if (trainDirsArr[d].isDirectory()) {
                    File curTrainFiles[] = trainDirsArr[d].listFiles();
                    Log.d(TAG, "Train dir:" + trainDirsArr[d].getName());
                    Log.d(TAG, "Train size:" + curTrainFiles.length);
                    trainLengthsList.add(curTrainFiles.length);
                    for (int i = 0; i < curTrainFiles.length; ++i) {
                        trainImgsList.add(curTrainFiles[i].getAbsoluteFile().toString());
                    }
                }
            }
            // Move data to arrays
            trainLengths = new int[trainLengthsList.size()];
            for (int i = 0; i < trainLengthsList.size(); ++i) {
                trainLengths[i] = trainLengthsList.get(i);
            }
            trainImagesFiles = trainImgsList.toArray(new String[trainImgsList.size()]);
        }

        // Test data

        // Length of test data
        int testLength = 0;
        {
            String testPath = String.format(Locale.getDefault(), Environment
                    .getExternalStorageDirectory().toString()
                    + "/FastABLE"
                    + "/test/");

            File testDir = new File(testPath);
            File testFiles[] = testDir.listFiles();
            Log.d(TAG, "Test size: " + testFiles.length);

            testLength = testFiles.length;
            testImagesFiles = new String[testFiles.length];
            for (int i = 0; i < testFiles.length; ++i) {
                testImagesFiles[i] = testFiles[i].getAbsoluteFile().toString();
            }
        }

        // Run NDK function to compute results
        float[] res = NDKTestFASTABLE(configFile,
                                trainImagesFiles,
                                trainLengths,
                                testImagesFiles,
                                testLength);

        Log.d(TAG, "RESULT: oa = " + res[0] + "\t fa = " + res[1]);
	}

}
