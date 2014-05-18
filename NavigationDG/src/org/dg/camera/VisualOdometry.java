package org.dg.camera;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.Normalizer;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Environment;
import android.util.Log;

public class VisualOdometry implements Runnable {

	private Mat kmeansCenters;
	private Mat[] bufImgs = new Mat[20];
	private Mat[] words = new Mat[20];

	public int keypointsDetected, descriptionSize, detectionTime,
			descriptionTime;
	public int trackingTime,  matchingTime;
	public int trackingSize, matchingSize1, matchingSize2;
	public int RANSACTime, RANSACCorrect;

	static {
		System.loadLibrary("VisualOdometryModule");
	}

	public VisualOdometry() {
	
	}

	// PEMRA
	@Override
	public void run() {
		
//		testingFivePoint();
		Log.d("PEMRA", "Started");

		File root = Environment.getExternalStorageDirectory();

		int setSize = 1;
		int startTime, difference;
		try {
			for (int i = 1; i <= setSize; i++) {
				Log.d("PEMRA", "Start reading");

				int classCount = 14;
				
				double resizingTime = 0 ;
				double wordTime = 0;
				for (int a = 0; a < classCount; a++) {
					// Read image
					String name = String.format("%d.jpg", a);
					File file = new File(root, "_exp/PEMRA_ref/" + name);
					Log.d("PEMRA", "Path :" + file.getAbsolutePath());
	
					Mat img = Highgui.imread(file.getAbsolutePath()), dst = new Mat();
	
					Log.d("PEMRA", "Img size:" + img.height() + "x" + img.width());

					// Resizing to 640x480
					startTime = (int) System.currentTimeMillis();
					Imgproc.resize(img, dst, new Size(480,640));
					//dst = img;
					difference = (int) (System.currentTimeMillis() - startTime);
					resizingTime += difference;
					Log.d("PEMRA", "Resize time :" + difference + " ms");
					
	
					Log.d("PEMRA", "Dst size:" + dst.height() + "x" + dst.width());

					// JAVA part
					bufImgs[a] = new Mat();
					detectDescript(dst.getNativeObjAddr(), bufImgs[a].getNativeObjAddr());

					Log.d("PEMRA", "Size of desc: " + bufImgs[a].rows() + "x"+ bufImgs[a].cols());
				}
				
				Log.d("PEMRA", "Average resizing time: " + resizingTime/14);
				//int K = 100;
				//int attempts = 10;
				//Mat bestLabels = new Mat();
				// http://docs.opencv.org/modules/core/doc/clustering.html
				//Mat dataIn = desc;
				// kmeans(dataIn, K, bestLabels, TermCriteria.MAX_ITER,
				// attempts, Core.KMEANS_PP_CENTERS);
				
				kmeansCenters = new Mat();
				int K = 20;
				kmeans(K, classCount);
				for (int a = 0; a < classCount; a++) {
					startTime = (int) System.currentTimeMillis();
					words[a] = createWord(bufImgs[a], K);
					difference = (int) (System.currentTimeMillis() - startTime);
					wordTime += difference;
					String wordzik = words[a].dump();
					Log.d("PEMRA", "Word: " + wordzik +"\n");
				}
				Log.d("PEMRA", "Average word creation time: " + wordTime/14);
				
				

				// Find 1 image in each wifi scan
//				ArrayList<File> inFiles = new ArrayList<File>();
//				File parentDir = new File(root, "_exp/PEMRA/");
//
//				for (File file1 : parentDir.listFiles()) {
//					if (file1.isDirectory()) {
//
//						File[] files = file1.listFiles();
//						inFiles.add(files[0]);
//					}
//				}
//
//				File file = new File(root, "_exp/PEMRA/result"+ K +".out");
//				FileOutputStream outStream = new FileOutputStream(file.getAbsolutePath());
//				PrintStream resultStream = new PrintStream(outStream);
//				
//				// Process images
//				int g=1;
//				for (File file1 : inFiles) {
//						
//					Log.d("PEMRA", "Processing image, number: " + g + "\n");
//					Log.d("PEMRA", "Processing image, name: " + file1.getAbsolutePath());
//					
//					Mat img = Highgui.imread(file1.getAbsolutePath()), dst = new Mat();
//					// Resizing to 640x480
//					Imgproc.resize(img, dst, new Size(480,640));
//					// JAVA part
//					Mat desc = new Mat();
//					detectDescript(dst.getNativeObjAddr(), desc.getNativeObjAddr());
//
//					//Log.d("PEMRA", "Size of desc: " + desc.rows() + "x"+ desc.cols());
//					
//					Mat wordToTest = createWord(desc, K);
//				
//					resultStream.print(file1.getParentFile().getName() + "\t" );
//					for (int a = 0; a < classCount; a++) {
//						double tmp = Core.norm(wordToTest, words[a], Core.NORM_L2);
//						resultStream.print(tmp + "\t");
//						//Log.d("PEMRA", "Error to class " + (a + 1) + " is : "
//						//		+ tmp);
//					}
//					resultStream.print("\n");
//					g++;
//				}
//				resultStream.close();

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			Log.e("PEMRA", "Error: " + e.getMessage());
		}
		Log.e("TEST", "Ended computation");
		
	}
	
	Mat createWord ( Mat descriptors, int k  )
	{
		// For each descriptor
		double [] word = new double[k];
		
		for (int i=0;i<descriptors.rows();i++)
		{
			Mat descToLabel = descriptors.row(i);
			
			int bestLabel = -1;
			double bestError = 0;
			
			// For each kmeans center
			for (int j=0;j<kmeansCenters.rows();j++)
			{
				Mat descCenter = kmeansCenters.row(j);
				double error = 0;						
				
				error = Core.norm(descCenter,descToLabel, Core.NORM_L2);
				
				if ( error < bestError || bestLabel == -1)
				{
					bestLabel = j;
					bestError = error;
				}
			}
			word[bestLabel]+= 1;
		}
		
		// Normalize word
		Mat wordTmp = new Mat(1, k, CvType.CV_32F);
		wordTmp.put(0, 0, word);
		Mat normalizedWord = new Mat();
		Core.normalize(wordTmp, normalizedWord);
		
		return normalizedWord;
	}

	// FivePointTests
	void testingFivePoint() {
		Mat points1 = new Mat(500, 2, CvType.CV_32FC1);
		Mat points2 = new Mat(500, 2, CvType.CV_32FC1);

		for (int j = 1; j < 3; j++) {
			int i = 0;
			try {
				String fileName = Environment.getExternalStorageDirectory()
						.toString() + "/_exp/fivepoint/points" + j + ".txt";
				FileInputStream inStream = new FileInputStream(fileName);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inStream, "UTF-8"));

				while (true) {
					String line = reader.readLine();
					if (line == null)
						break;
					String[] fields = line.split(" ");

					Log.d("FivePoint",
							"Points " + j + ": "
									+ Double.parseDouble(fields[0]) + " "
									+ Double.parseDouble(fields[1]));
					if (j == 1) {
						points1.put(i, 0, Double.parseDouble(fields[0]));
						points1.put(i, 1, Double.parseDouble(fields[1]));
					} else {
						points2.put(i, 0, Double.parseDouble(fields[0]));
						points2.put(i, 1, Double.parseDouble(fields[1]));
					}
					i++;
				}
				reader.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//testFivePoint(points1.getNativeObjAddr(), points2.getNativeObjAddr());
	}

	// IWCMC / ICIAR
	public void detectDescribe(Mat img, int N, int detector, int descriptor) {
		// Single threaded
		if ( N == 1)
			detectDescribeFeatures(img.getNativeObjAddr(), detector, descriptor);
		// Multi-threaded
		else
			parallelDetectDescribeFeatures(img.getNativeObjAddr(), N,  detector, descriptor);
	}
	
	
	public void trackingMatching(Mat image, Mat image2, int keypointSize, int N, int detector, int descriptor)
	{
		trackingMatchingTest(image.getNativeObjAddr(), image2.getNativeObjAddr(), keypointSize, N, detector, descriptor);
	}
	
//	public void tracking(Mat image, Mat image2, int N, int detector)
//	{
//		parallelTrackingTest(image.getNativeObjAddr(), image2.getNativeObjAddr(), N, detector);
//	}

	
	public void RANSAC(Mat points1, Mat points2, int numOfThreads, int nPoint)
	{
		RANSACTest(points1.getNativeObjAddr(), points2.getNativeObjAddr(), numOfThreads, nPoint);
	}
	
	// PEMRA
	public native int detectDescript(long matAddrImg, long matAddrDescriptors);

	public native void kmeans(int k, int referenceCount);

	// REST

	

	public native void detectDescribeFeatures(long matAddrGr, int param, int param2);
	
	public native void parallelDetectDescribeFeatures(long matAddrGr, int N, int param, int param2);
	
	public native void trackingMatchingTest(long image, long image2, int keypointSize, int N, int param, int param2);
	
	public native void parallelTrackingTest(long image, long image2, int N, int param);
	
	public native int RANSACTest(long matAddrPoints1, long matAddrPoints2, int numOfThreads, int nPoint);

}
