// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Michal Nowicki (michal.nowicki@put.poznan.pl)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.dg.obsolete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
//import org.opencv.imgcodecs.*;



import android.os.Environment;
import android.util.Log;

public class DetectDescribe implements Runnable {

	static final String MScLog = "MScThesis";
	private static final boolean DETECTOR_DESCRIPTOR_TEST = false;
	private static final boolean TRACKING_MATCHING_TEST = false;
	private static final boolean RANSAC_NPOINT_TEST = false;
	private static final boolean TRAJECTORY_TEST = true;
	private static final int MAX_NUIMBER_OF_TEST_IMAGES = 5; // MAX 613 for
																// DESK sequence

	private static final int LDB = 0;

	private static final int[] detectors = { FeatureDetector.FAST,
			FeatureDetector.STAR, FeatureDetector.SIFT, FeatureDetector.SURF,
			FeatureDetector.ORB, FeatureDetector.GFTT, FeatureDetector.HARRIS, };

	private static final int[] descriptors = { DescriptorExtractor.SIFT,
			DescriptorExtractor.SURF, DescriptorExtractor.ORB,
			DescriptorExtractor.BRIEF, DescriptorExtractor.BRISK,
			DescriptorExtractor.FREAK, LDB };

	private static final String[] detectorNames = { "FAST", "STAR", "SIFT",
			"SURF", "ORB", "GFTT", "HARRIS" };
	private static final String[] descriptorNames = { "SIFT", "SURF", "ORB",
			"BRIEF", "BRISK", "FREAK", "LDB" };

	@Override
	public void run() {
		android.os.Process.setThreadPriority(0);

		VisualOdometry vo = new VisualOdometry();

		try {

			// For all files in dir
			ArrayList<File> inFiles = new ArrayList<File>();
			File root = Environment.getExternalStorageDirectory();
			File parentDir = new File(root, "_exp/detectDescribe/desk");

			int numberOfImages = 0;
			for (File file1 : parentDir.listFiles()) {
				if (!file1.isDirectory()) {

					inFiles.add(file1);
					numberOfImages++;
					if (numberOfImages >= MAX_NUIMBER_OF_TEST_IMAGES)
						break;
				}
			}

			if (DETECTOR_DESCRIPTOR_TEST) {
				detectorDescriptorTest(vo, inFiles, root);
			}

			if (TRACKING_MATCHING_TEST) {
				trackingMatchingTest(vo, inFiles, root);
			}

			if (RANSAC_NPOINT_TEST)
				nPointTest(vo, root);
			
			
			if (TRAJECTORY_TEST) {
				
				trajectoryTest(vo);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.d("DetectDescribe", "Caught exception - " + e.toString());
			e.printStackTrace();
		}
		
	}

	private void trajectoryTest(VisualOdometry vo)
			throws FileNotFoundException {
		File root = Environment.getExternalStorageDirectory();
		File parentDir;
		Log.d(MScLog, "Started trajectory test");
		
		File file = new File(root, "_exp/detectDescribe/TrajectoryTest.log");
		FileOutputStream outStream = new FileOutputStream(
				file.getAbsolutePath());
		PrintStream trajectoryStream = new PrintStream(outStream);
		
		File file2 = new File(root, "_exp/detectDescribe/ScaleEstimate.log");
		FileOutputStream outStream2 = new FileOutputStream(
				file2.getAbsolutePath());
		PrintStream scaleStream = new PrintStream(outStream2);

	
		parentDir = new File(root, "_exp/detectDescribe/desk");
		//parentDir = new File(root, "_exp/detectDescribe/set002");
		
		File[] files = parentDir.listFiles();
		Arrays.sort(files);
		int imgCounter = 0;
		Log.d(MScLog, "Started reading images");
		int kk=0;
		for (File file1 : files) {

			if ( true && kk > 0 ) //|| kk % 3 == 0 )// && kk > 270)
			{
				Log.d(MScLog, "img: " + file1.toString());
				// Read image
				// Opencv 3.0.0: Mat img = Imgcodecs.imread(file1.getAbsolutePath());
				Mat img = Highgui.imread(file1.getAbsolutePath());
				vo.imagesToProcess[imgCounter] = img.clone();
				img.release();
				imgCounter++;
			}
			
			if (imgCounter > 47)
				break;
			kk++;
		}

		Log.d(MScLog, "Images read successfully");
		//
		// NDK call
		//
		// 1 thread, 	ORB-ORB (4-2)
		//				SURF-SURF (3-1)
		vo.trajectoryTest(1,
				detectors[3],
				descriptors[1], imgCounter);
		
		Mat me = vo.getMotionEstimate();
		Mat sc = vo.getScaleEstimate();
		Log.d("DetectDescribe", "Sizes - " + me.cols() + " " + me.rows() + " " + sc.cols() + " " + sc.rows());
		
		for (int i=0;i<imgCounter;i++)
		{
			Log.d("DetectDescribe", "Save iter: " + i);
			trajectoryStream.print(Integer.toString(i) + " ");
			for (int j=0;j<7;j++)
			{
				double[] x = me.get(j, i);
				trajectoryStream.print(Double.toString(x[0]) + " ");
			}
			trajectoryStream.print("\n");
			
			double[] x = sc.get(0,i);
			scaleStream.print(Double.toString(x[0]) + "\n");
			vo.releaseImg(i);
		}
		me.release();
		sc.release();
		
		scaleStream.close();
		trajectoryStream.close();
		Log.d("DetectDescribe", "Ended computation");
	}

	private void detectorDescriptorTest(VisualOdometry vo,
			ArrayList<File> inFiles, File root) throws FileNotFoundException {
		// FAST - BRIEF
		// FAST - LDB
		// SIFT - SIFT
		// SURF - SURF
		// ORB - ORB
		// GFTT - BRIEF
		// HARRIS - BRIEF
		int[][] pairsToEval = { { 0, 3 }, { 0, 6 }, { 2, 0 }, { 3, 1 },
				{ 4, 2 }, { 5, 3 }, { 6, 3 } };

		int[] numOfThreadsToEval = { 1, 2, 3, 4, 5, 6 };
		// int [] numOfThreadsToEval = {1};

		// Files to save results
		File file = new File(root,
				"_exp/detectDescribe/desk_detectDescribe.log");
		FileOutputStream outStream = new FileOutputStream(
				file.getAbsolutePath());
		PrintStream detDeskStream = new PrintStream(outStream);

		detDeskStream
				.print("Detector\tDescriptor\tNumber of threads\tDetection time\tNumber of detected keypoints\tDescription time\tDescription time per keypoint\tDetection + desk of 500 keypoints\n");

		// For all pairs
		for (int pairCounter = 0; pairCounter < pairsToEval.length; pairCounter++) {
			int detectorIndex = pairsToEval[pairCounter][0];
			int descriptorIndex = pairsToEval[pairCounter][1];

			// For all thread numbers
			for (int numOfThreadsIndex = 0; numOfThreadsIndex < numOfThreadsToEval.length; numOfThreadsIndex++) {

				double detectionTime = 0;
				double keypointsDetected = 0;

				double descriptionTime = 0;
				double descriptionSize = 0;
				double descriptionTimePerKeypoint = 0;

				int numOfThreads = numOfThreadsToEval[numOfThreadsIndex];

				// For all images
				int imgCounter = 0;

				Mat lastImage = null;
				for (File file1 : inFiles) {

					// Read image
					// Opencv 3.0.0: Mat img = Imgcodecs.imread(file1.getAbsolutePath());
					Mat img = Highgui.imread(file1.getAbsolutePath());
					
					if (lastImage == null) {
						lastImage = img.clone();
					}

					imgCounter++;

					//
					// NDK call
					//
					vo.detectDescribe(img, numOfThreads,
							detectors[detectorIndex],
							descriptors[descriptorIndex]);

					// Accumulate results
					detectionTime += vo.detectionTime;
					keypointsDetected += vo.keypointsDetected;

					descriptionTime += vo.descriptionTime;
					descriptionSize += vo.descriptionSize;
					descriptionTimePerKeypoint += ((double) vo.descriptionTime)
							/ vo.descriptionSize;

					// Print where we are with processing
					Log.d("DetectDescribe", "Dettime: " + vo.detectionTime);
					Log.d("DetectDescribe", "DetDeskTest: "
							+ detectorNames[detectorIndex] + " "
							+ descriptorNames[descriptorIndex] + " N="
							+ numOfThreads + " imgCounter="
							+ imgCounter);

					// Release java memory
					lastImage.release();
					lastImage = img.clone();
					img.release();
				}
				lastImage.release();

				// Save results
				detDeskStream.print(detectorNames[detectorIndex]
						+ "\t"
						+ descriptorNames[descriptorIndex]
						+ "\t"
						+ numOfThreads
						+ "\t"
						+ Double.toString(detectionTime / imgCounter)
						+ "\t"
						+ Double.toString(keypointsDetected
								/ imgCounter)
						+ "\t"
						+ Double.toString(descriptionTime / imgCounter)
						+ "\t"
						// + Double.toString(descriptionSize /
						// imgCounter)
						// + "\t"
						+ Double.toString(descriptionTimePerKeypoint
								/ imgCounter)
						+ "\t"
						+ Double.toString(detectionTime / imgCounter
								+ 500 * descriptionTimePerKeypoint
								/ imgCounter) + "\n");
			}

		}
		detDeskStream.close();
	}

	private void trackingMatchingTest(VisualOdometry vo,
			ArrayList<File> inFiles, File root) throws FileNotFoundException {
		// FAST - BRIEF
		// FAST - LDB
		// SIFT - SIFT
		// SURF - SURF
		// ORB - ORB
		// GFTT - BRIEF
		// HARRIS - BRIEF
		int[][] pairsToEval = { { 0, 3 }, { 0, 6 }, { 2, 0 }, { 3, 1 },
				{ 4, 2 }, { 5, 3 }, { 6, 3 } };

		int[] numOfThreadsToEval = { 1, 2, 4 };

		int[] keypointSizes = { 200, 500, 1000, 2000 };

		File file = new File(root,
				"_exp/detectDescribe/desk_trackingMatching.log");
		FileOutputStream outStream = new FileOutputStream(
				file.getAbsolutePath());
		PrintStream trackMatchStream = new PrintStream(outStream);

		trackMatchStream
				.print("Detector\tDescriptor\tKeypointSize\tNumber of threads\tTracking time\tNumber of tracked points\tMatching time\tNumber of matched features 1\tNumber of matched features 2\n");

		// For all pairs
		for (int pairCounter = 0; pairCounter < pairsToEval.length; pairCounter++) {
			int detectorIndex = pairsToEval[pairCounter][0];
			int descriptorIndex = pairsToEval[pairCounter][1];

			for (int keypointIndex = 0; keypointIndex < keypointSizes.length; keypointIndex++) {
				int keypointSize = keypointSizes[keypointIndex];

				int numOfThreadsLength = 1;
				if (keypointSize == 500) {
					numOfThreadsLength = numOfThreadsToEval.length;
				}

				// For all thread numbers
				for (int numOfThreadsIndex = 0; numOfThreadsIndex < numOfThreadsLength; numOfThreadsIndex++) {
					double trackingTime = 0;
					double trackingSize = 0;
					double matchingTime = 0;
					double matchingSize1 = 0;
					double matchingSize2 = 0;

					int numOfThreads = numOfThreadsToEval[numOfThreadsIndex];

					// For all images
					int imgCounter = 0;

					Mat lastImage = null;
					for (File file1 : inFiles) {

						// Read image
						// Opencv 3.0.0: Mat img = Imgcodecs.imread(file1
						//		.getAbsolutePath());
						Mat img = Highgui.imread(file1
								.getAbsolutePath());
						if (lastImage == null) {
							lastImage = img.clone();
						}

						imgCounter++;

						//
						// NDK
						//
						// if (numOfThreads == 1) {
						vo.trackingMatching(lastImage, img,
								keypointSize, numOfThreads,
								detectors[detectorIndex],
								descriptors[descriptorIndex]);
						matchingTime += vo.matchingTime;
						matchingSize1 += vo.matchingSize1;
						matchingSize2 += vo.matchingSize2;
						// } else
						// vo.tracking(lastImage, img, numOfThreads,
						// detectors[detectorIndex]);

						trackingTime += vo.trackingTime;
						trackingSize += vo.trackingSize;

						// Print where we are with processing
						Log.d("DetectDescribe", "TrackMatchTest: "
								+ detectorNames[detectorIndex] + " "
								+ descriptorNames[descriptorIndex]
								+ " Keypoints=" + keypointSize + " N="
								+ numOfThreads + " imgCounter="
								+ imgCounter);

						// Release memory
						lastImage.release();
						lastImage = img.clone();
						img.release();
					}
					lastImage.release();

					// Save results
					trackMatchStream
							.print(detectorNames[detectorIndex]
									+ "\t"
									+ descriptorNames[descriptorIndex]
									+ "\t"
									+ keypointSize
									+ "\t"
									+ numOfThreads
									+ "\t"
									+ Double.toString(trackingTime
											/ imgCounter)
									+ "\t"
									+ Double.toString(trackingSize
											/ imgCounter)
									+ "\t"
									+ Double.toString(matchingTime
											/ imgCounter)
									+ "\t"
									+ Double.toString(matchingSize1
											/ imgCounter)
									+ "\t"
									+ Double.toString(matchingSize2
											/ imgCounter) + "\n");
				}
			}

		}
		trackMatchStream.close();
	}

	private void nPointTest(VisualOdometry vo, File root)
			throws FileNotFoundException, UnsupportedEncodingException,
			IOException {
		File parentDir;
		{
			int results[] = new int[25];
			
			//int[] nPoints = { 5, 8};
			int[] nPoints = { 5 };
			//int[] numOfThreadsToEval = { 1, 2, 4 };
			int[] numOfThreadsToEval = { 1 };
			
			File file = new File(root, "_exp/detectDescribe/RANSACTest.log");
			FileOutputStream outStream = new FileOutputStream(
					file.getAbsolutePath());
			PrintStream RANSACStream = new PrintStream(outStream);

			// For all files in dir
			root = Environment.getExternalStorageDirectory();
			parentDir = new File(root, "_exp/detectDescribe/nPoints");

			File[] files = parentDir.listFiles();
			Arrays.sort(files, Collections.reverseOrder());
			for (File file1 : files) {

				for (int nPointIndex = 0; nPointIndex <  nPoints.length ; nPointIndex++) {
					int nPoint = nPoints[nPointIndex];

					for (int numOfThreadsIndex = 0; numOfThreadsIndex < numOfThreadsToEval.length
																		 ; numOfThreadsIndex++) {
						int numOfThreads = numOfThreadsToEval[numOfThreadsIndex];

						if (file1.getName().contains("points1")) {
							// if ( file1.getName().contains("0.00") )
							// {
							Mat points1 = new Mat(500, 2, CvType.CV_32FC1);
							readNPointFile(file1, points1);

							File file2 = new File(
									parentDir.getAbsolutePath()
											+ "/"
											+ file1.getName().replace(
													"points1", "points2"));

							Mat points2 = new Mat(500, 2, CvType.CV_32FC1);
							readNPointFile(file2, points2);
							
							Log.d("DetectDescribe",
									"Read "
											+ file1.getName().subSequence(8, 11)
											+ ": "
											+ Integer.toString(points1
													.rows())
											+ " + "
											+ Integer.toString(points2
													.rows()));

//								for (int kk = 0; kk < 5; kk++) {
//									double[] x = points1.get(kk, 0);
//									double[] y = points1.get(kk, 1);
//									Log.d("DetectDescribe", "Point: " + x[0]
//											+ " " + y[0] + "\n");
//								}
//
//								for (int kk = 0; kk < 5; kk++) {
//									double[] x = points2.get(kk, 0);
//									double[] y = points2.get(kk, 1);
//									Log.d("DetectDescribe", "Point2: " + x[0]
//											+ " " + y[0] + "\n");
//
//								}

							vo.RANSAC(points1, points2, numOfThreads,
									nPoint);
							double RANSACTime = vo.RANSACTime;
							int RANSACCorrect = vo.RANSACCorrect;

							RANSACStream.print(Integer
									.toString(numOfThreads)
									+ "\t"
									+ Integer.toString(nPoint)
									+ "\t"
									+ file1.getName().subSequence(8, 12)
									+ "\t"
									+ Double.toString(RANSACTime)
									+ "\t"
									+ Double.toString(RANSACCorrect)
									+ "\n");

							points1.release();
							points2.release();
							// }
							

						}
					}

				}
			}

			RANSACStream.close();
		}
	}

	private void readNPointFile(File file, Mat points)
			throws FileNotFoundException, UnsupportedEncodingException,
			IOException {
		Log.d("DetectDescribe", "Reading " + file.getAbsolutePath());
		FileInputStream inStream = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				inStream, "UTF-8"));

		int i = 0;
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;
			String[] fields = line.split(" ");

			points.put(i, 0, Double.parseDouble(fields[0]));
			points.put(i, 1, Double.parseDouble(fields[1]));
			i++;

		}

		reader.close();
	}

}
