#include <jni.h>
#include <vector>

#include "EigenUnsupported/FFT"
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

	// We just log the window size to make sure that everything is ok
	__android_log_print(ANDROID_LOG_DEBUG, "Stepometer",
									"Window size: %d\n", processingWindowSize);

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

	// Some debug information
	__android_log_print(ANDROID_LOG_DEBUG, "Stepometer", "NDK fft:  max index : %d, max value: %f \n", indMax, greatestVal);
	__android_log_print(ANDROID_LOG_DEBUG, "Stepometer", "NDK fft:  found frequency: %f \n", foundFreq);

	// Clean after yourself
	delete[] accWindow;

	// Return frequency

	return foundFreq;
}

}

