package org.dg.inertialSensors;


import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.util.Log;

public class AHRSModule {
	
	// Calls to the native part of the code
	public native long EKFcreate(float Q, float R, float dt);
	public native void EKFpredict(long addrEKF, long addrW, float dt);
	public native void EKFcorrect(long addrEKF, long addrZ);
	public native void EKFdestroy(long addrEKF);
	
	// Variable holding the address of the EKF initialized in native code
	private long addrEKF;
	private boolean predictionStart = false;
	private long startTime;
	
	// It is called on the class initialization
	static {
		System.loadLibrary("AHRSModule");
	}
	
	public AHRSModule() {
		 // We create the EKF
		 createEKF(0.0001f, 10f, 0.01f);
	}
	
	public AHRSModule(float Q, float R, float dt) {
		 // We create the EKF
		createEKF(Q, R, dt);
	}
	
	private void createEKF(double Q, double R, double dt) {
		addrEKF = EKFcreate((float)Q, (float)R, (float)dt);
		Log.d("EKF", "EKF created succesfully!\n");
	}
	
	
	// CvType.CV_32F -> Gyroscope data
	public void predict(float gyroX, float gyroY, float gyroZ)
	{
		//Log.d("EKF", "EKF predict start!\n");
		
		Mat gyroMeasurement = Mat.zeros(3,1, CvType.CV_32F);
		gyroMeasurement.put(0, 0, gyroX);
		gyroMeasurement.put(1, 0, gyroY);
		gyroMeasurement.put(2, 0, gyroZ);
		
		if(!predictionStart)
			startTime = System.currentTimeMillis();
		
		double timeDifference = (System.currentTimeMillis() - startTime)/1000;
		
		EKFpredict(addrEKF, gyroMeasurement.getNativeObjAddr(), (float)timeDifference);
		startTime = System.currentTimeMillis();
	}

	// CvType.CV_32F -> Rotation quaternion
	public void correct(float quatW, float quatX, float quatY, float quatZ)
	{
		//Log.d("EKF", "EKF correct start!\n");		
	
		Mat rotationMeasurement = Mat.zeros(4,1, CvType.CV_32F);
		rotationMeasurement.put(0, 0, quatW);
		rotationMeasurement.put(1, 0, quatX);
		rotationMeasurement.put(2, 0, quatY);
		rotationMeasurement.put(3, 0, quatZ);
		EKFcorrect(addrEKF, rotationMeasurement.getNativeObjAddr());
	}
	
	public void destroy()
	{
		EKFdestroy(addrEKF);
	}
	
}
