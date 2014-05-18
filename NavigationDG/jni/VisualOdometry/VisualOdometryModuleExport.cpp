#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>


#include "NonFree/nonfree/features2d.hpp"
#include "NonFree/nonfree/nonfree.hpp"

#include <vector>
#include <android/log.h>

#include "FivePoint/FivePointAlgorithm.h"
//#include "../FivePoint/FivePointAlgorithm.h"
#include "FivePointMadeEasy/5point.h"
#include "DetectDescribe/DetectDescribe.h"

#include "LDB/ldb.h"

using namespace std;
using namespace cv;

#define DEBUG_TAG "NDK_MainActivity"
#define DEBUG_TAG_PEMRA "PEMRA"
#define DEBUG_TAG_DETDES "DetectDescribe"


//#define DEBUG_MODE

extern "C" {

// Export declarations

// PEMRA
JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_detectDescript(JNIEnv*,
		jobject, jlong addrImg, jlong addrDescriptors);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_kmeans(JNIEnv* env,
		jobject thisObject, int k, int referenceCount);



// IWCMC/ICIAR

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_detectDescribeFeatures(JNIEnv*,
		jobject, jlong addrGray, jint param, jint param2);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_parallelDetectDescribeFeatures(JNIEnv*,
		jobject, jlong addrGray, jint N, jint param, jint param2);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_trackingMatchingTest(JNIEnv*,
		jobject, jlong addrGray, jlong addrGray2, jint keypointSize, jint N, jint param, jint param2);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_parallelTrackingTest(JNIEnv*,
		jobject, jlong addrGray, jlong addrGray2, jint N, jint param);

JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_RANSACTest(JNIEnv*,
		jobject, jlong addrPoints1, jlong addrPoints2, jint numOfThreads, jint Npoint);


// Implementation

// PEMRA
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


// ICIAR / IWCMC
JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_detectDescribeFeatures(
		JNIEnv* env, jobject thisObject, jlong addrGray, jint param, jint param2) {

	cv::setNumThreads(1);

	struct timeval start;
	struct timeval end;

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Started detection/description test\n");
#endif

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);

	// Get methods and fields
	jfieldID detectionTimeID = env->GetFieldID(thisclass, "detectionTime", "I");
	jfieldID keypointsDetectedID = env->GetFieldID(thisclass, "keypointsDetected", "I");

	jfieldID descriptionTimeID = env->GetFieldID(thisclass, "descriptionTime", "I");
	jfieldID descriptionSizeID = env->GetFieldID(thisclass, "descriptionSize", "I");


#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
			"Accessed local variables\n");
#endif

	Mat& mGr = *(Mat*) addrGray;


	/// Single-threaded detection
	vector<KeyPoint> v;
	gettimeofday(&start, NULL);
	DetectDescribe::performDetection(mGr,v,param);
	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
				- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
	env->SetIntField(thisObject, detectionTimeID, ret);
	env->SetIntField(thisObject, keypointsDetectedID, v.size());

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
				"Measured detection time : %d \n", ret);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
				"Found keypoints : %d \n", v.size());
#endif

	/// Single-threaded description
	cv::Mat desc;
	gettimeofday(&start, NULL);
	DetectDescribe::performDescription(mGr,v,desc,param2);
	gettimeofday(&end, NULL);

	ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	env->SetIntField(thisObject, descriptionTimeID, ret);
	env->SetIntField(thisObject, descriptionSizeID, desc.rows);

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
					"Measured description time : %d \n", ret);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
					"Measured description size : %d \n", desc.rows);
#endif

}

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_parallelDetectDescribeFeatures(JNIEnv* env,
		jobject thisObject, jlong addrGray, jint N, jint param, jint param2)
{
	cv::setNumThreads(1);

	struct timeval start;
	struct timeval end;

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Started parallel detection/description test\n");
#endif

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);

	// Get methods and fields
	jfieldID detectionTimeID = env->GetFieldID(thisclass, "detectionTime", "I");
	jfieldID keypointsDetectedID = env->GetFieldID(thisclass,
			"keypointsDetected", "I");

	jfieldID descriptionTimeID = env->GetFieldID(thisclass, "descriptionTime",
			"I");
	jfieldID descriptionSizeID = env->GetFieldID(thisclass, "descriptionSize",
			"I");

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
			"Accessed local variables\n");
#endif

	Mat& mGr = *(Mat*) addrGray;

	/// Parallel detection
	DetectDescribe * parallelDetectDescribe = new DetectDescribe(mGr, N, 15);

	gettimeofday(&start, NULL);
		int sizeOfParallelKeypoints = parallelDetectDescribe->performParallelDetection(param);
	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
	env->SetIntField(thisObject, detectionTimeID, ret);
	env->SetIntField(thisObject, keypointsDetectedID, sizeOfParallelKeypoints);

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
			"Measured parallel detection time : %d \n", ret);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
				"Parallel detection keypoints : %d \n", sizeOfParallelKeypoints);
#endif

	/// Parallel description
	gettimeofday(&start, NULL);
		parallelDetectDescribe->performParallelDescription(param2);
	gettimeofday(&end, NULL);

	ret = ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
	env->SetIntField(thisObject, descriptionTimeID, ret);

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
						"Measured parallel description time : %d \n", ret);
#endif

	/// Time taken to gather parallel results
	std::vector<cv::KeyPoint> v;
	v = parallelDetectDescribe->getKeypointsFromParallelProcessing();
	cv::Mat x = parallelDetectDescribe->getDescriptorsFromParallelProcessing();

	env->SetIntField(thisObject, descriptionSizeID, x.rows);

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
							"Parallel descriptors size :%d\n", x.rows);
#endif

	/// TODO:
	/// PARALLEL DETECTION AND DESCRIPTION AS ONE FUNCTION

	delete parallelDetectDescribe;
}

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_trackingMatchingTest(
		JNIEnv* env, jobject thisObject, jlong addrGray, jlong addrGray2, jint keypointSize, jint N, jint param, jint param2) {

	cv::setNumThreads(N);

//#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Started tracking/matching test, N= %d\n", cv::getNumThreads());
//#endif

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);

	// Get methods and fields
	jfieldID trackingTimeID = env->GetFieldID(thisclass, "trackingTime", "I");
	jfieldID trackingSizeID = env->GetFieldID(thisclass, "trackingSize", "I");

	jfieldID matchingTimeID = env->GetFieldID(thisclass, "matchingTime", "I");
	jfieldID matchingSize1ID = env->GetFieldID(thisclass, "matchingSize1", "I");
	jfieldID matchingSize2ID = env->GetFieldID(thisclass, "matchingSize2", "I");

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
			"Accessed local variables\n");
#endif

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
				"N:%d, KeypointSize:%d, det:%d, desk:%d\n",N, keypointSize, param, param2);
#endif

	Mat& image = *(Mat*) addrGray;
	Mat& image2 = *(Mat*) addrGray2;


	vector<KeyPoint> v, v2, vTmp;
	cv::Mat desc, desc2;


	DetectDescribe::performDetection(image, v, param);
	DetectDescribe::performDetection(image2, v2, param);

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
					"Sizes: %d %d\n", v.size(), v2.size());
#endif

	// If it is not HARRIS and GFTT
	if (param!= 7 && param != 8)
	{
		DetectDescribe::performDescription(image,v,desc,param2);
		DetectDescribe::performDescription(image2,v2,desc2,param2);

		while(v.size() < keypointSize)
		{
			int a = rand() % v.size();
			v.push_back(v[a]);
			cv::Mat x = (desc.row(a)).clone();
			desc.push_back(x);
		}

		while (v.size() > keypointSize) {
			v.pop_back();
			desc.pop_back();
		}

		while (v2.size() < keypointSize) {
			int a = rand() % v2.size();
			v2.push_back(v2[a]);
			cv::Mat x = (desc2.row(a)).clone();
			desc2.push_back(x);
		}

		while (v2.size() > keypointSize) {
			v2.pop_back();
			desc2.pop_back();
		}
	}
#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
						"Sizes: %d %d\n", v.size(), v2.size());
#endif


	struct timeval start;
	struct timeval end;


	int ret = 0;

	std::vector<cv::Point2f> points2Track, temp = std::vector<cv::Point2f>();
	// IF FAST+BRIEF, GFTT or HARRIS
	if ( (param == 1 && param2 == 4) || param == 7 || param == 8)
	{
		std::vector<uchar> status;
		std::vector<float> err;
		cv::TermCriteria termcrit(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 15, 0.05);


		cv::KeyPoint::convert(v, points2Track);

	#ifdef DEBUG_MODE
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
								"Points to track : %d \n", points2Track.size());
	#endif

		gettimeofday(&start, NULL);
		// Calculating movement of features
		// Max lvl - 0
			cv::calcOpticalFlowPyrLK(image, image2, points2Track, temp, status, err, cvSize(5,5), 0, termcrit);
		gettimeofday(&end, NULL);

		ret = ((end.tv_sec * 1000000 + end.tv_usec)
				- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
	}

	env->SetIntField(thisObject, trackingTimeID, ret);
	env->SetIntField(thisObject, trackingSizeID, points2Track.size());

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
					"Measured tracking time : %d \n", ret);
#endif

	ret = 0;

	// If it is not HARRIS and GFTT
	if ( param!= 7 && param != 8)
	{
		cv::BFMatcher *matcher;
		switch (param2) {
		case 0:
		case 3:
		case 4:
		case 5:
			matcher = new cv::BFMatcher(NORM_HAMMING, true);
//			matcher = new cv::BFMatcher(NORM_L2, true);
			break;
		case 1:
		case 2:
		case 6:
		default:
			matcher = new cv::BFMatcher(NORM_L2, true);
		}

		gettimeofday(&start, NULL);
			std::vector< cv::DMatch > matches;
			matcher->match( desc, desc2, matches );
		gettimeofday(&end, NULL);
		delete matcher;

		ret = ((end.tv_sec * 1000000 + end.tv_usec)
				- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
	}

	env->SetIntField(thisObject, matchingTimeID, ret);
	env->SetIntField(thisObject, matchingSize1ID, desc.rows);
	env->SetIntField(thisObject, matchingSize2ID, desc2.rows);

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
			"Measured matching time : %d  | %d %d\n", ret, desc.rows, desc2.rows);
#endif

}

JNIEXPORT void JNICALL Java_org_dg_camera_VisualOdometry_parallelTrackingTest(JNIEnv* env,
		jobject thisObject, jlong addrGray, jlong addrGray2, jint N, jint param)
{
	cv::setNumThreads(4);

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Started parallel tracking/matching test\n");
#endif

		// Find the required classes
		jclass thisclass = env->GetObjectClass(thisObject);

		// Get methods and fields
		jfieldID trackingTimeID = env->GetFieldID(thisclass, "trackingTime", "I");
		jfieldID trackingSizeID = env->GetFieldID(thisclass, "trackingSize", "I");

		jfieldID matchingTimeID = env->GetFieldID(thisclass, "matchingTime", "I");
		jfieldID matchingSize1ID = env->GetFieldID(thisclass, "matchingSize1", "I");
		jfieldID matchingSize2ID = env->GetFieldID(thisclass, "matchingSize2", "I");

		env->SetIntField(thisObject, matchingTimeID, 0);
		env->SetIntField(thisObject, matchingSize1ID, 0);
		env->SetIntField(thisObject, matchingSize2ID, 0);

#ifdef DEBUG_MODE
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
				"Accessed local variables\n");
#endif


		Mat& image = *(Mat*) addrGray;
		Mat& image2 = *(Mat*) addrGray2;


		struct timeval start;
		struct timeval end;

		// Parallel tracking
		DetectDescribe * parallelDetectDescribe = new DetectDescribe(image, image2, N, 15);


		int sizeOfParallelKeypoints =
			parallelDetectDescribe->performParallelDetection(param);

		gettimeofday(&start, NULL);
			parallelDetectDescribe->performParallelTracking();
		gettimeofday(&end, NULL);

		delete parallelDetectDescribe;

		int ret = ((end.tv_sec * 1000000 + end.tv_usec)
				- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
		env->SetIntField(thisObject, trackingTimeID, ret);
		env->SetIntField(thisObject, trackingSizeID, sizeOfParallelKeypoints);


#ifdef DEBUG_MODE
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
							"Measured parallel tracking time : %d \n", ret);
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES,
									"Measured parallel tracking size : %d \n",sizeOfParallelKeypoints);
#endif
}


// REST
#define DEBUG_MODE
JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_RANSACTest(JNIEnv* env,
		jobject thisObject, jlong addrPoints1, jlong addrPoints2, jint numOfThreads, jint Npoint) {

	cv::setNumThreads(1);
	struct timeval start;
	struct timeval end;

#ifdef DEBUG_MODE
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Started RANSAC test\n");
#endif

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);

	// Get methods and fields
	jfieldID RANSACTimeID = env->GetFieldID(thisclass, "RANSACTime", "I");
	jfieldID RANSACCorrectID = env->GetFieldID(thisclass, "RANSACCorrect", "I");


	/// Started N-point processing
	Mat& points1 = *(Mat*) addrPoints1;
	Mat& points2 = *(Mat*) addrPoints2;
	Mat rotation;
	Mat translation;

	gettimeofday(&start, NULL);
		FP::RotationTranslationFromFivePointAlgorithm(points2, points1, 1. / 500., numOfThreads, Npoint, rotation, translation);
		//FP::RotationTranslationFromFivePointAlgorithm(points2, points1, 1. / 500., rotation, translation);
	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
	env->SetIntField(thisObject, RANSACTimeID, ret);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "RANSAC time: %d\n", ret);

	if ( numOfThreads == 1)
	{
		double err = (translation.at<double>(0) - 0.7)*(translation.at<double>(0) - 0.7) +
		 (translation.at<double>(1) + 0.3)*(translation.at<double>(0) + 0.3) +
		 (translation.at<double>(2) - 0.6)*(translation.at<double>(2) - 0.6);
	//	err = sqrt(err);
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "RANSAC error: %f\n", err);
		if ( abs(err) < 0.015 )
		{
			env->SetIntField(thisObject, RANSACCorrectID, 1);
		}
		else
		{
			env->SetIntField(thisObject, RANSACCorrectID, 0);
		}

	//	std::vector<EMatrix> ret_E;
	//	std::vector<PMatrix> ret_P;
	//	std::vector<int> ret_inliers;

		//Solve5PointEssential(pts1, pts2, 5, ret_E, ret_P, ret_inliers);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Five point rot: %f %f %f",
				rotation.at<double>(0, 0), rotation.at<double>(0, 1),
				rotation.at<double>(0, 2));
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Five point rot: %f %f %f",
				rotation.at<double>(1, 0), rotation.at<double>(1, 1),
				rotation.at<double>(1, 2));
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Five point rot: %f %f %f",
				rotation.at<double>(2, 0), rotation.at<double>(2, 1),
				rotation.at<double>(2, 2));
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DETDES, "Five point translation: %f %f %f",
				translation.at<double>(0), translation.at<double>(1),
				translation.at<double>(2));
	}
}

}

