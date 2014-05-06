#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>


#include "NonFree/nonfree/features2d.hpp"
#include "NonFree/nonfree/nonfree.hpp"

#include <vector>
#include <android/log.h>

//#include "FivePoint/FivePointAlgorithm.h"
//#include "../FivePoint/FivePointAlgorithm.h"
#include "FivePointMadeEasy/5point.h"

using namespace std;
using namespace cv;

#define DEBUG_TAG "NDK_MainActivity"
#define DEBUG_TAG_PEMRA "PEMRA"

extern "C" {

// Export declarations

// PEMRA
JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_detectDescript(JNIEnv*,
		jobject, jlong addrImg, jlong addrDescriptors);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_kmeans(JNIEnv* env,
		jobject thisObject, int k, int referenceCount);

// REST
JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_testFivePoint(JNIEnv*,
		jobject, jlong addrPoints1, jlong addrPoints2);

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_detectFeatures(JNIEnv*,
		jobject, jlong addrGray, jint param);

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_descriptFeatures(JNIEnv*,
		jobject, jlong addrGray, jint param, jint param2);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_kmeans(JNIEnv* env,
		jobject thisObject, int k, int referenceCount) {

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
					"kmeans started\n");

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);
	jclass matclass = env->FindClass("org/opencv/core/Mat");

	// Get methods and fields
	jmethodID getPtrMethod = env->GetMethodID(matclass, "getNativeObjAddr",
			"()J");
	jfieldID kmeansCentersfieldid = env->GetFieldID(thisclass, "kmeansCenters", "Lorg/opencv/core/Mat;");
	jfieldID bufimgsfieldid = env->GetFieldID(thisclass, "bufImgs",
			"[Lorg/opencv/core/Mat;");

	// Let's start: Get the fields
	jobject javakmeansCenters = env->GetObjectField(thisObject, kmeansCentersfieldid);
	jobjectArray bufimgsArray = (jobjectArray) env->GetObjectField(thisObject,
			bufimgsfieldid);

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
						"kmeans converting array\n");

	// Convert the array
	cv::Mat nativeBufImgs[referenceCount];
	for (int i = 0; i < referenceCount; i++)
		nativeBufImgs[i] = *(cv::Mat*) env->CallLongMethod(
				env->GetObjectArrayElement(bufimgsArray, i), getPtrMethod);

	 cv::Mat& kmeansCenters = *(cv::Mat*)env->CallLongMethod(javakmeansCenters, getPtrMethod);

	 __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
	 						"kmeans we have access to local variables\n");

	// We have an access to Mat of descriptors
	Mat bestLabels;
	// http://docs.opencv.org/modules/core/doc/clustering.html
	int height = 0, width = nativeBufImgs[0].cols;
	for (int i = 0; i < referenceCount; i++)
		height += nativeBufImgs[i].rows;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
				"kmeans dataIn size : %dx%d\n", height, width);

	cv::Mat dataIn(height, width, CV_32FC1);
	height = 0;
	for (int i = 0; i<referenceCount;i++)
	{
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
				"kmeans keypoints size : %dx%d\n", nativeBufImgs[i].rows,
				nativeBufImgs[i].cols);
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
				"kmeans rect size : (%d, %d, %d, %d)\n", 0, height,
				nativeBufImgs[i].cols, nativeBufImgs[i].rows);
		Mat roi(dataIn,
				Rect(0, height, nativeBufImgs[i].cols, nativeBufImgs[i].rows));
		nativeBufImgs[i].copyTo(roi); //copy old image to upper area
		height += nativeBufImgs[i].rows;
	}


	struct timeval start;
	struct timeval end;

	gettimeofday(&start, NULL);
	kmeans(dataIn, k, bestLabels, cv::TermCriteria(), 5,
			cv::KMEANS_PP_CENTERS, kmeansCenters);
	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
			"kmeans taken : %d ms\n", ret);

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
				"kmeans 1st val : %f %f %f\n", kmeansCenters.at<float>(0,0), kmeansCenters.at<float>(0,1), kmeansCenters.at<float>(0,2));

}

bool keypointResponseCompare(cv::KeyPoint i, cv::KeyPoint j) {
	return i.response > j.response;
}

JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_detectDescript(JNIEnv*,
		jobject, jlong addrImg, jlong addrDescriptors) {
	Mat& img = *(Mat*) addrImg;
	Mat& descriptors = *(Mat*) addrDescriptors;

	struct timeval start;
	struct timeval end;

	SurfFeatureDetector detector;
	vector<KeyPoint> v;

	gettimeofday(&start, NULL);
	detector.detect(img, v);
	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
			"Detection time : %d ms\n", ret);

	sort(v.begin(), v.end(), keypointResponseCompare);
	v.resize(500);

	SurfDescriptorExtractor extractor;

	gettimeofday(&start, NULL);
	extractor.compute(img, v, descriptors);
	gettimeofday(&end, NULL);

	ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_PEMRA,
			"Description time : %d ms\n", ret);

}

JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_testFivePoint(JNIEnv*,
		jobject, jlong addrPoints1, jlong addrPoints2) {
	Mat& points1 = *(Mat*) addrPoints1;
	Mat& points2 = *(Mat*) addrPoints2;
	Mat rotation;
	Mat translation;

	struct timeval start;
	struct timeval end;

	gettimeofday(&start, NULL);
//	FP::RotationTranslationFromFivePointAlgorithm(points2, points1, 1. / 500.,
//			rotation, translation);

	std::vector<EMatrix> ret_E;
	std::vector<PMatrix> ret_P;
	std::vector<int> ret_inliers;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point: Now print size");
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point pts size: %d x %d ",
				points1.rows, points1.cols);
	double pts1[10];
	double pts2[10];
	for(int i=0;i<10;i++)
	{
		pts1[i] = points1.at<float>(i/2,i%5);
		pts2[i] = points2.at<float>(i/2,i%5);
	}
//
	for(int i=0;i<1000;i++)
		Solve5PointEssential(pts1, pts2, 5, ret_E, ret_P, ret_inliers);
	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point time : %d ms",
			ret);

//	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
//			rotation.at<double>(0, 0), rotation.at<double>(0, 1),
//			rotation.at<double>(0, 2));
//	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
//			rotation.at<double>(1, 0), rotation.at<double>(1, 1),
//			rotation.at<double>(1, 2));
//	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
//			rotation.at<double>(2, 0), rotation.at<double>(2, 1),
//			rotation.at<double>(2, 2));
//	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Five point : %f %f %f",
//			translation.at<double>(0), translation.at<double>(1),
//			translation.at<double>(2));
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

