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

package org.dg.inertialSensors;

import android.util.Log;

public class AHRSModule {
	// It is called on the class initialization
	static {
		System.loadLibrary("AHRSModule");
		Log.d("EKF", "EKF lib loaded!\n");
	}

	// Definitions of methods available in NDK
	public native long EKFcreate(float Qqmin, float Qwmin, float Qqmax,
			float Qwmax, float Rr);

	public native float[] EKFpredict(long addrEKF, float[] input, float dt);

	public native float[] EKFcorrect(long addrEKF, float[] measurement);

	public native void EKFdestroy(long addrEKF);

	// Address of EKF instance
	private long addrEKF;

	// Most up-to-date estimate
	float[] orientationEstimate = { 0.0f, 0.0f, 0.0f, 0.0f };

	public AHRSModule() {
		final float Qqmin = (float) (2.528 * Math.pow(10, -7));
		final float Qwmin = (float) (4.483 * Math.pow(10, -7));
		final float Qqmax = (float) (9.342 * Math.pow(10, -7));
		final float Qwmax = (float) (8.159 * Math.pow(10, -7));
		final float Rr = 3.672f;
		create(Qqmin, Qwmin, Qqmax, Qwmax, Rr);
		//create(Qqmax, Qwmax, Qqmax, Qwmax, Rr);
	}

	public AHRSModule(float Qqmin, float Qwmin, float Qqmax, float Qwmax,
			float Rr) {
		// Experimentally found values
		// - for slow and steady motions:
		// Qq = 2.528 * 10^7, Qw = 4.483 * 10^7, Rr = 3.672
		// - for dynamic motions:
		// Qq = 9.342 * 10^7, Qw = 8.159 * 10^7, Rr = 3.672
		create(Qqmin, Qwmin, Qqmax, Qwmax, Rr);
	}

	private void create(float Qqmin, float Qwmin, float Qqmax, float Qwmax,
			float Rr) {
		addrEKF = EKFcreate(Qqmin, Qwmin, Qqmax, Qwmax, Rr);
	}

	public void predict(float wx, float wy, float wz, float dt) {
		// w is 3x1 gyro measurement
		float[] w = new float[3];
		w[0] = wx;
		w[1] = wy;
		w[2] = wz;

		orientationEstimate = EKFpredict(addrEKF, w, dt);
	}

	public void correct(float q1, float q2, float q3, float q4) {
		// z is 4x1 quat orientation
		float[] z = new float[4];
		z[0] = (float) q1;
		z[1] = (float) q2;
		z[2] = (float) q3;
		z[3] = (float) q4;

		orientationEstimate = EKFcorrect(addrEKF, z);
	}

	public float[] getEstimate() {
		return orientationEstimate;
	}

	public void destroy() {
		EKFdestroy(addrEKF);
	}

}
