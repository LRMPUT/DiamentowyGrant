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
