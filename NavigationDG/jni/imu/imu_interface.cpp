#include <jni.h>
#include <vector>

#include "include/orientationEstKalman.h"
#include <unistd.h>

extern "C" {

// Export declarations
JNIEXPORT void JNICALL Java_org_dg_main_MainActivity_IMU(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

JNIEXPORT void JNICALL Java_org_dg_main_MainActivity_IMU(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
	orientationEstSystem *orientSystem = new orientationEstKalman();

	float rand_data[3] = {0,0,0};
	const int frequency = 20;
	const unsigned long int sleep_time = 1000000/frequency;
	while(1)
	{
		// Updating based on measurements
		orientSystem->updateAcc(rand_data);
		orientSystem->updateGyro(rand_data);
		orientSystem->updateMag(rand_data);

		// getting the estimate
		orientSystem->getEstimate(rand_data);

		usleep(sleep_time);
	}
	delete orientSystem;
}

}

