#include <jni.h>
#include <vector>

#include "EigenUnsupported/FFT"
//#include "../fftw3/api/fftw3.h"


using namespace std;


extern "C" {

#define DEBUG_TAG "NDK_MainActivity"


// Export declarations
JNIEXPORT int JNICALL Java_org_dg_inertialSensors_Stepometer_fftTest(JNIEnv*,
		jobject);

JNIEXPORT int JNICALL Java_org_dg_inertialSensors_Stepometer_fftTest(JNIEnv*,
		jobject) {


	struct timeval start;
	struct timeval end;


	Eigen::FFT<float> fft;

	std::vector<float> timevec;
	for (int i=0;i<512;i++)
	{
		timevec.push_back(sin(i/5));
	}
	std::vector<std::complex<float> > freqvec;


	gettimeofday(&start, NULL);
	// Time measured operation
	for (int i=0;i<10000;i++)
		fft.fwd( freqvec,timevec);
	fft.inv( timevec,freqvec);

//	int N = 1024;
//	fftwf_complex *in, *out;
//	fftwf_plan p;
//
//	in = (fftwf_complex*) fftwf_malloc(sizeof(fftwf_complex) * N);
//	out = (fftwf_complex*) fftwf_malloc(sizeof(fftwf_complex) * N);
//	//p = fftwf_plan_dft_1d(N, in, out, FFTW_FORWARD, FFTW_ESTIMATE);
//	fftwf_plan_dft(1, (int const*)1024 ,in, out, FFTW_FORWARD, FFTW_ESTIMATE);
//
//	fftwf_execute(p); /* repeat as needed */
//
//	fftwf_destroy_plan(p);
//	fftwf_free(in); fftwf_free(out);

	gettimeofday(&end, NULL);


	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	return ret;
}

}

