package org.dg.camera;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.util.Log;

public class scaleEstimation {

	public native long EKFcreate(float Q, float R, float dt);
	public native long EKFgetState();
	public native void EKFpredict(long addrEKF, float dt);
	public native void EKFcorrectAcc(long addrEKF, long addrZ);
	public native void EKFcorrectVision(long addrEKF, long addrZ);
	public native void EKFcorrectQR(long addrEKF, long addrZ);
	public native void EKFdestroy(long addrEKF);
	
	
	private long addrEKF;
	
	public scaleEstimation() {
		 System.loadLibrary("ScaleEstimation");
		 Log.d("ScaleEstimation", "ScaleEstimation lib loaded!\n");
	}
	
	
	public void create()
	{
		addrEKF = EKFcreate(0.0001f, 10f, 0.01f);
		Log.d("EKF", "EKF created succesfully!\n");
	}
	
	public long getState()
	{
		return EKFgetState();
	}
	
	public void predict()
	{
		Log.d("EKF", "EKF predict start!\n");
		for (int i=0;i<10000;i++)
		{		
			EKFpredict(addrEKF, 0.01f);
		}
		long timeEnd = System.currentTimeMillis();
		Log.d("EKF", "EKF predict time : " + timeEnd + "!\n");
	}

	public void correctAcc(float accZ)
	{
		// z is a measurement matrix
		Mat z = Mat.zeros(1,1, CvType.CV_32F);
		z.put(0, 0, accZ);
		
		Log.d("EKF", "EKF correct start!\n");
		EKFcorrectAcc(addrEKF, z.getNativeObjAddr());
	}
	
	public void correctVision(float estimateX)
	{
		// z is a measurement matrix
		Mat z = Mat.zeros(1,1, CvType.CV_32F);
		z.put(0, 0, estimateX);
		
		Log.d("EKF", "EKF correct start!\n");
		EKFcorrectVision(addrEKF, z.getNativeObjAddr());
	}
	
	public void correctQR(float estimateScale)
	{
		// z is a measurement matrix
		Mat z = Mat.zeros(1,1, CvType.CV_32F);
		z.put(0, 0, estimateScale);
		
		Log.d("EKF", "EKF correct start!\n");
		EKFcorrectQR(addrEKF, z.getNativeObjAddr());
	}
	
	public void destroy()
	{
		EKFdestroy(addrEKF);
	}
}
