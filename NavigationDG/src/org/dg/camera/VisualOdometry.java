package org.dg.camera;

import org.opencv.core.Mat;

public class VisualOdometry {
	
	static {
		System.loadLibrary("visual_odometry");
		System.loadLibrary("nonfree");
	}
	
	public VisualOdometry() {
		
	}
	
	public int detect(Mat img, int j)
	{
		if ( j == 3)
		{
			return SiftFeatures(img.getNativeObjAddr(), j);
		}
		else if ( j == 4)
		{
			return SurfFeatures(img.getNativeObjAddr(), j);
		}
		return detectFeatures(img.getNativeObjAddr(), j);
	}

	
	public native int detectFeatures(long matAddrGr, int param);
	public native int descriptFeatures(long matAddrGr, int param, int param2);
	
	public native int SurfFeatures(long matAddrGr, int param);
	public native int SurfDescription(long matAddrGr, int param);
	public native int SiftFeatures(long matAddrGr, int param);
	public native int SiftDescription(long matAddrGr, int param);
}
