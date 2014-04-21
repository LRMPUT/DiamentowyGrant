package com.example.cam;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.spec.DESKeySpec;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.os.Environment;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class DetectDescript implements Runnable {

	int priority = 0;
	int detectorType = 0;
	Mat image;

	// values between 0 and 10
	public void setPriority(int value) {
		priority = value;
	}

	@Override
	public void run() {
		Log.d("PEMRA", "Started");

		File root = Environment.getExternalStorageDirectory();

		int setSize = 1;
		int startTime, difference;
		try {
			//
			//

			// Creating detectors and descriptors
			FeatureDetector detector = null;
			detector = FeatureDetector.create(FeatureDetector.FAST);//FeaFeatureDetector.create(0);
			Log.d("PEMRA", "Detector created");
			
			MatOfKeyPoint keypoints = new MatOfKeyPoint();

			DescriptorExtractor extractor = DescriptorExtractor
					.create(DescriptorExtractor.BRIEF);

			for (int i = 1; i <= setSize; i++) {
				double oneTime = 0;
				Log.d("PEMRA", "Start reading");
				
				// Read image
				String name = String.format("%04d.jpg", i);
				File file = new File(root, "_exp/PEMRA/" + name);
				Log.d("PEMRA", "Path :" + file.getAbsolutePath());
				
				Mat img = Highgui.imread(file.getAbsolutePath()), dst = new Mat();
				
				Log.d("PEMRA", "Img size:" + img.height() + "x" + img.width());
				
				// Resizing to 640x480
				//Imgproc.resize(img, dst, new Size(480,640));
				dst = img;
				
				Log.d("PEMRA", "Dst size:" + dst.height() + "x" + dst.width());

				// JAVA part
				startTime = (int) System.currentTimeMillis();
				detector.detect(dst, keypoints);
				difference = (int) (System.currentTimeMillis() - startTime);
				Log.d("PEMRA", "Detector time :" + difference + " ms");
				
				
				oneTime += difference;

				int siz = (keypoints.toList()).size();

				Log.d("PEMRA", "Detector found :" + siz + " keypoints");
				// if ( k == 0)
				// {
				// det_java.writeUTF(Integer.toString(difference) + "\t"
				// + Integer.toString(siz) + nl);
				// }
				
				
				Log.d("PEMRA", "Descriptor start");
				startTime = (int) System.currentTimeMillis();
				Mat desc = new Mat();
				extractor.compute(dst, keypoints, desc);
				difference = (int) (System.currentTimeMillis() - startTime);
				Log.d("PEMRA", "Descriptor time: " + difference + " ms");

				Log.d("PEMRA", "Size of descriptor: " + desc.height() + "x" + desc.width());
				
				
				int K = 100;
				int attempts = 10;
				Mat bestLabels = new Mat();
				// http://docs.opencv.org/modules/core/doc/clustering.html
				Mat dataIn = desc;
		//		kmeans(dataIn, K, bestLabels, TermCriteria.MAX_ITER, attempts, Core.KMEANS_PP_CENTERS);
				
				
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.e("PEMRA", "Error: " + e.getMessage());
		}
		Log.e("TEST", "Ended computation");
	}

}
