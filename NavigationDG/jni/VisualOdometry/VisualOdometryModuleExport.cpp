#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <android/log.h>

#include "FivePoint/FivePointAlgorithm.h"
//#include "../FivePoint/FivePointAlgorithm.h"

using namespace std;
using namespace cv;

#define DEBUG_TAG "NDK_MainActivity"

extern "C" {

// Export declarations
JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_testFivePoint(JNIEnv*,
		jobject, jlong addrPoints1, jlong addrPoints2);

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_detectFeatures(JNIEnv*,
		jobject, jlong addrGray, jint param);
JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_descriptFeatures(JNIEnv*,
		jobject, jlong addrGray, jint param, jint param2);

JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_testFivePoint(JNIEnv*,
		jobject, jlong addrPoints1, jlong addrPoints2) {
	Mat& points1 = *(Mat*) addrPoints1;
	Mat& points2 = *(Mat*) addrPoints2;
	Mat rotation;
	Mat translation;

	struct timeval start;
	struct timeval end;

	gettimeofday(&start, NULL);
	FP::RotationTranslationFromFivePointAlgorithm(points2, points1, 1. / 500.,
			rotation, translation);
	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point time : %d ms",
			ret);

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
			rotation.at<double>(0, 0), rotation.at<double>(0, 1),
			rotation.at<double>(0, 2));
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
			rotation.at<double>(1, 0), rotation.at<double>(1, 1),
			rotation.at<double>(1, 2));
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
			rotation.at<double>(2, 0), rotation.at<double>(2, 1),
			rotation.at<double>(2, 2));
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
			translation.at<double>(0), translation.at<double>(1),
			translation.at<double>(2));
}

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_detectFeatures(JNIEnv*,
		jobject, jlong addrGray, jint param) {
	Mat& mGr = *(Mat*) addrGray;

	FeatureDetector *detector;
	vector<KeyPoint> v;

	switch (param) {
	case 1:
		detector = new cv::FastFeatureDetector();
		break;
	case 2:
		detector = new cv::StarFeatureDetector();
		break;
	case 5:
		detector = new cv::ORB();
		break;
	case 6:
		detector = new cv::MSER();
		break;
	case 7:
		detector = new cv::GFTTDetector();
		break;
	case 8:
		detector = new cv::GFTTDetector(1000, 0.01, 1., 3, true, 0.04);
		break;
	case 9:
		detector = new SimpleBlobDetector();
		break;
	case 10:
		detector = new cv::DenseFeatureDetector();
		break;
	default:
		detector = new cv::FastFeatureDetector();
	}

	struct timeval start;
	struct timeval end;

	gettimeofday(&start, NULL);

	detector->detect(mGr, v);

	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	return ret;
}

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_descriptFeatures(
		JNIEnv* env, jobject, jlong addrGray, jint param, jint param2) {
	Mat& mGr = *(Mat*) addrGray;

	FeatureDetector *detector;
	vector<KeyPoint> v;

	switch (param) {
	case 1:
		detector = new cv::FastFeatureDetector();
		break;
	case 2:
		detector = new cv::StarFeatureDetector();
		break;
	case 5:
		detector = new cv::ORB();
		break;
	case 6:
		detector = new cv::MSER();
		break;
	case 7:
		detector = new cv::GFTTDetector();
		break;
	case 8:
		detector = new cv::GFTTDetector(1000, 0.01, 1., 3, true, 0.04);
		break;
	case 9:
		detector = new SimpleBlobDetector();
		break;
	case 10:
		detector = new cv::DenseFeatureDetector();
		break;
	default:
		detector = new cv::FastFeatureDetector();
	}

	detector->detect(mGr, v);

	DescriptorExtractor *extractor;

	switch (param2) {
	case 3:
		extractor = new cv::ORB();
		break;
	case 4:
		extractor = new cv::BriefDescriptorExtractor();
		break;
	case 5:
		extractor = new cv::BRISK();
		break;
	case 6:
		extractor = new cv::FREAK();
		break;
	default:
		extractor = new cv::ORB();
	}

	cv::Mat desc;

	struct timeval start;
	struct timeval end;

	gettimeofday(&start, NULL);

	extractor->compute(mGr, v, desc);

	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	delete detector;
	delete extractor;

	return ret;
}

}

