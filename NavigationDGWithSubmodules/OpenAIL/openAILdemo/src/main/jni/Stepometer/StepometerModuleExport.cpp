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

#include <jni.h>
#include <vector>

#include "../3rdParty/EigenUnsupported/FFT"
#include "android/log.h"
//#include "../fftw3/api/fftw3.h"


using namespace std;


extern "C" {

#define DEBUG_TAG "NDK_MainActivity"


// Export declarations
JNIEXPORT float JNICALL Java_org_dg_inertialSensors_Stepometer_fftFindDominantFrequency(JNIEnv* env,
		jobject, jfloatArray processingWindowArray, jfloat accelerometerMeasurementFrequency) {


	// Getting an array of acceletometer measurements from JAVA
	jsize processingWindowSize = env->GetArrayLength(processingWindowArray);
	float * accWindow  = new float[processingWindowSize];
	env->GetFloatArrayRegion(processingWindowArray,0,processingWindowSize,accWindow);

	// We need to convert float* to vector<float> -> do it smartly with c++11
	std::vector<float> accWindowVec{accWindow, accWindow + processingWindowSize};

	// Using Eigen::fft implementation
	Eigen::FFT<float> fft;

	// Vector to store the signal in the frequency domain
	std::vector<std::complex<float> > freqvec;

	// Do the FFT
	fft.fwd( freqvec, accWindowVec);

	// Find the dominant frequency index
	// -> checking only half the spectrum
	int indMax = 0;
	float greatestVal = std::abs(freqvec[0]);
	for (int i=1;i<=processingWindowSize/2;i++)
	{
		if ( std::abs(freqvec[i]) > greatestVal )
		{
			indMax = i;
			greatestVal = std::abs(freqvec[i]);
		}
	}

	// Compute dominant frequency
	float foundFreq = indMax * accelerometerMeasurementFrequency / processingWindowSize;

	// Clean after yourself
	delete[] accWindow;

	// Return frequency
	return foundFreq;
}

}

