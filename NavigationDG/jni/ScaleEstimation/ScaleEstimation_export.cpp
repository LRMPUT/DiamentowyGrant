#include <jni.h>
#include <opencv2/core/core.hpp>
#include <vector>

#include "ScaleEKF/ScaleEKF.h"

using namespace std;
using namespace cv;

extern "C" {

//
// Export declarations
//

// Create object
JNIEXPORT jlong JNICALL Java_org_dg_main_scaleEstimation_EKFcreate(JNIEnv*, jobject, jfloat Q,
		jfloat R, jfloat dt);

// get estimate
JNIEXPORT jlong JNICALL Java_org_dg_main_scaleEstimation_EKFgetEstimate(JNIEnv*, jobject, jlong addrEKF);

// Predict
JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFpredict(JNIEnv*, jobject,
		jlong addrEKF, jlong addrW, jfloat dt);

// Update from: Acc, Vision, QR
JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFcorrectAcc(JNIEnv*, jobject,
		jlong addrEKF, jlong addrZ);
JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFcorrectVision(JNIEnv*, jobject,
		jlong addrEKF, jlong addrZ);
JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFcorrectQR(JNIEnv*, jobject,
		jlong addrEKF, jlong addrZ);

// Destroy object
JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFdestroy(JNIEnv*, jobject,
		jlong addrEKF);


//
// Implementation of export methods
//
JNIEXPORT jlong JNICALL Java_org_dg_main_scaleEstimation_EKFcreate(JNIEnv*, jobject, jfloat Q,
		jfloat R, jfloat dt) {

	// Create new object
	return (long) (new EKF(Q, R, dt));
}

JNIEXPORT jlong JNICALL Java_org_dg_main_scaleEstimation_EKFgestEstimate(JNIEnv*, jobject, jlong addrEKF)
{
	EKF &ekf = *(EKF*) addrEKF;
	cv::Mat estimate = ekf.getEstimate();
	return estimate.at<float>(3);
}

JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFpredict(JNIEnv*, jobject,
		jlong addrEKF, jlong addrW, jfloat dt) {

	// Calling predict
	EKF &ekf = *(EKF*) addrEKF;
	ekf.predict(dt);
}

JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFcorrectAcc(JNIEnv*, jobject,
		jlong addrEKF, jlong addrZ) {

	// Calling correct
	EKF &ekf = *(EKF*) addrEKF;
	ekf.correctAcc(addrZ);
}

JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFcorrectVison(JNIEnv*, jobject,
		jlong addrEKF, jlong addrZ) {

	// Calling correct
	EKF &ekf = *(EKF*) addrEKF;
	ekf.correctVision(addrZ);
}

JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFcorrectQR(JNIEnv*, jobject,
		jlong addrEKF, jlong addrZ) {

	// Calling correct
	EKF &ekf = *(EKF*) addrEKF;
	ekf.correctQR(addrZ);
}

JNIEXPORT void JNICALL Java_org_dg_main_scaleEstimation_EKFdestroy(JNIEnv* env, jobject,
		jlong addrEKF) {

	// Destroy object
	delete (EKF*) (addrEKF);
}

}

