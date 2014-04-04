package org.dg.camera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.os.Environment;
import android.util.Log;

public class VisualOdometry implements Runnable {

	static {
		System.loadLibrary("VisualOdometryModule");
	}

	public VisualOdometry() {

	}

	@Override
	public void run()
	{
		Mat points1 = new Mat(500,2,CvType.CV_32FC1);
		Mat points2 = new Mat(500,2,CvType.CV_32FC1);
		
		for (int j=1;j<3;j++)
		{
			int i=0;
			try {
				String fileName = Environment.getExternalStorageDirectory().toString() + "/_exp/fivepoint/points"+j+".txt";	
				FileInputStream inStream = new FileInputStream(fileName);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
				
				while (true) {
				    String line = reader.readLine();
					if (line == null)
						break;
					String[] fields = line.split(" ");
					
					
					Log.d("FivePoint","Points " + j + ": "+ Double.parseDouble(fields[0]) + " " + Double.parseDouble(fields[1]));
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
			
		
		testFivePoint(points1.getNativeObjAddr(), points2.getNativeObjAddr());
	}

	public int detect(Mat img, int j) {
		if (j == 3) {
			return SiftFeatures(img.getNativeObjAddr(), j);
		} else if (j == 4) {
			return SurfFeatures(img.getNativeObjAddr(), j);
		}
		return detectFeatures(img.getNativeObjAddr(), j);
	}

	public native int testFivePoint(long matAddrPoints1, long matAddrPoints2);

	public native int detectFeatures(long matAddrGr, int param);

	public native int descriptFeatures(long matAddrGr, int param, int param2);

	public native int SurfFeatures(long matAddrGr, int param);

	public native int SurfDescription(long matAddrGr, int param);

	public native int SiftFeatures(long matAddrGr, int param);

	public native int SiftDescription(long matAddrGr, int param);
}
