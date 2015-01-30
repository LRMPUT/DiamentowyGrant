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
#include "Triangulate/triangulate.h"

#include "VOsystem/VOsystem.h"

#include "LDB/ldb.h"


using namespace std;
using namespace cv;

#define DEBUG_TAG "NDK_MainActivity"
#define DEBUG_TAG_PEMRA "PEMRA"
#define DEBUG_TAG_DETDES "DetectDescribe"
#define DEBUG_TAG_MSC "MScThesis"


#define DEBUG_MODE

extern "C" {

// Export declarations

// MSc thesis
JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_estimateTrajectory(JNIEnv* env,
		jobject thisObject, jint numOfThreads, jint detector, jint descriptor, jint size);

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



/// MSc thesis
JNIEXPORT int JNICALL Java_org_dg_camera_VisualOdometry_estimateTrajectory(JNIEnv* env,
		jobject thisObject, jint numOfThreads, jint detectorType, jint descriptorType, jint size)
{
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
						"Native code started\n");

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);
	jclass matclass = env->FindClass("org/opencv/core/Mat");

	// Get methods and fields
	jmethodID getPtrMethod = env->GetMethodID(matclass, "getNativeObjAddr",
				"()J");
	jfieldID bufimgsfieldid = env->GetFieldID(thisclass, "imagesToProcess",
				"[Lorg/opencv/core/Mat;");
	jfieldID motionEstimatefieldid = env->GetFieldID(thisclass, "motionEstimate",
					"Lorg/opencv/core/Mat;");
	jfieldID scaleEstimatefieldid = env->GetFieldID(thisclass, "scaleEstimate",
						"Lorg/opencv/core/Mat;");

	// Let's start: Get the fields
	jobject javaMotionEst = env->GetObjectField(thisObject, motionEstimatefieldid);
	jobject javaScaleEst = env->GetObjectField(thisObject, scaleEstimatefieldid);
	jobjectArray bufimgsArray = (jobjectArray) env->GetObjectField(thisObject,
				bufimgsfieldid);



	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_MSC,
							"Converting array\n");

	// Convert the array
	cv::Mat& motionEstimate = *(cv::Mat*)env->CallLongMethod(javaMotionEst, getPtrMethod);
	cv::Mat& scaleEstimate = *(cv::Mat*)env->CallLongMethod(javaScaleEst, getPtrMethod);

	cv::Mat imagesToProcess[size];
	for (int i = 0; i < size; i++)
	{
		imagesToProcess[i] = *(cv::Mat*) env->CallLongMethod(
				env->GetObjectArrayElement(bufimgsArray, i), getPtrMethod);
	}


	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
			"Successful access to local vars\n");


	/// Init variables
	motionEstimate = cv::Mat(7, size, CV_64FC1);
	scaleEstimate = cv::Mat(1, size, CV_64FC1);

	std::vector<cv::KeyPoint> v[3];
	cv::Mat descriptors[3];
	Mat prevRotation;
	Mat prevTranslation;


	double focal = 525.0;
	double cx = 319.5, cy = 239.5;
	double cameraParam[3][3] = {{319.5,0,525.0},{0,239.5, 525.0},{0,0,1}};
	//double focal = 1414.27;
	//double cx = 401.394, cy = 308.09;
	//double cameraParam[3][3] = {{focal,0,cx},{0, focal, cy},{0,0,1}};
	double cameraDist[5] = { 0.0, 0.0, 0.0, 0.0, 0.0 };
	//double cameraDist[5] = { -0.07714, 0.454159, 0.0000944, -0.0000424, 0.0};
	cv::Mat cameraMatrix = cv::Mat(3, 3, CV_64FC1, &cameraParam), distCoeffs = cv::Mat(1, 5, CV_64FC1, &cameraDist);

	const float inlierThreshold = 2.0;

	///
	/// Started processing !
	///

	// Two first images are processed differently
	for (int i=0;i<2;i++)
	{
		DetectDescribe::performDetection(imagesToProcess[i],
								v[i], detectorType);
		DetectDescribe::performDescription(imagesToProcess[i], v[i], descriptors[i],
								descriptorType);
	}
	std::vector<cv::DMatch> prevMatches;
    DetectDescribe::performMatching(descriptors[0], descriptors[1], prevMatches, descriptorType);

    // Assuming first scale
    double scale = 1;

    std::vector<cv::Point2f> vTmp1, vTmp2;
    for (int i=0;i<prevMatches.size();i++)
    {
    	vTmp1.push_back(v[0][prevMatches[i].queryIdx].pt);
    	vTmp2.push_back(v[1][prevMatches[i].trainIdx].pt);
    }
	int Npoint = 5;
	cv::Mat points1(vTmp1), points2(vTmp2), prevInliers;
	FP::RotationTranslationFromFivePointAlgorithm(points2, points1, inlierThreshold, numOfThreads, Npoint, prevRotation, prevTranslation, prevInliers, focal, cx, cy);

	Eigen::Matrix4f currentEstimate = VO::getInitialPosition(); // Eigen::Matrix4f::Identity();
	VO::eigen2matSave(currentEstimate, motionEstimate, 0);
	scaleEstimate.at<double>(0,0) = scale;

	prevTranslation = prevTranslation * scale;


	currentEstimate = currentEstimate * VO::mat2eigen(prevRotation, prevTranslation);
	VO::eigen2matSave(currentEstimate, motionEstimate, 1);
	scaleEstimate.at<double>(0,1) = scale;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
						"First estimation - inliers percent : %f\n", VO::countInlierRate(prevInliers));


	// 0 - triangle
	// 1 - triangulacja + PnP
	// 2 - no scale
	const int ESTIMATION_TYPE = 1;

	for (int i = 2; i < size; i++)
	{

		Mat rotation;
		Mat translation;
		Mat inliers;

		// Processing 3rd image
		DetectDescribe::performDetection(imagesToProcess[i], v[2], detectorType);
		DetectDescribe::performDescription(imagesToProcess[i], v[2], descriptors[2], descriptorType);

		// We match 2nd with 3rd
		std::vector<cv::DMatch> matches;
		DetectDescribe::performMatching(descriptors[1], descriptors[2], matches, descriptorType);

		// Extract potential inliers for 5-point algorithm
		cv::Mat p1, p2;
		VO::matchedPoints2Mat(matches, v[1], v[2], p1, p2);
		FP::RotationTranslationFromFivePointAlgorithm(p2, p1,
				inlierThreshold, numOfThreads, Npoint, rotation, translation, inliers, focal, cx, cy);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
								"Estimating 2-3: inliers percent: %f\n", VO::countInlierRate(inliers));

		// Triangle
		if ( ESTIMATION_TYPE == 0)
		{
			// Initialize some variables
			std::vector<cv::DMatch> matches13;
			Mat inliers13;
			cv::Mat rotation13, translation13;
			cv::Mat p13_1, p13_3;

			// Matching13 + essential matrix estimation
			DetectDescribe::performMatching(descriptors[0], descriptors[2], matches13, descriptorType);
			VO::matchedPoints2Mat(matches13, v[0], v[2], p13_1, p13_3);
			FP::RotationTranslationFromFivePointAlgorithm(p13_3, p13_1,
					inlierThreshold, numOfThreads, Npoint, rotation13, translation13, inliers13, focal, cx, cy);

			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
							"1-3 Inliers percent: %f \n", VO::countInlierRate(inliers13));


			// Scale estimation
			translation13 = prevRotation * translation13;
			scale = estimateScaleTriangle(prevTranslation, translation13, translation);
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
										"New scale : %f \n", scale);

			// Applying new scale
			//translation = translation * scale;

			// Saving trajectory and estimates
			currentEstimate = currentEstimate * VO::mat2eigen(rotation, translation);
			VO::eigen2matSave(currentEstimate, motionEstimate, i);
			scaleEstimate.at<double>(0,i) = scale;

			// Copying for next iteration
			translation.copyTo(prevTranslation);
			rotation.copyTo(prevRotation);
		}
		// From triangulation
		else if ( ESTIMATION_TYPE == 1)
		{
			// Initialize some variables
			std::vector<cv::DMatch> matches13;
			Mat inliers13;
			cv::Mat rotation13, translation13;
			cv::Mat p13_1, p13_3;

			// Matching13 + essential matrix estimation
			DetectDescribe::performMatching(descriptors[0], descriptors[2], matches13, descriptorType);
			VO::matchedPoints2Mat(matches13, v[0], v[2], p13_1, p13_3);
			FP::RotationTranslationFromFivePointAlgorithm(p13_3, p13_1,
				inlierThreshold, numOfThreads, Npoint, rotation13, translation13, inliers13, focal, cx, cy);

			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
				"1-3 Inliers percent: %f %f %f\n", VO::countInlierRate(prevInliers), VO::countInlierRate(inliers), VO::countInlierRate(inliers13));
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
							"vector sizes: %d %d %d\n", v[0].size(), v[1].size(), v[2].size());



			// 3-view inliers
			cv::Mat projPoints1, projPoints2, projPoints3;
			VO::matchedPoints3Mat(prevMatches, matches, matches13, v[0], v[1], v[2], prevInliers, inliers, inliers13, projPoints1, projPoints2, projPoints3);

//			cv::Mat projPoints1, projPoints2, projPoints3;
//			VO::matchedPoints3Mat(prevMatches, matches, v[0], v[1], v[2], prevInliers, inliers, projPoints1, projPoints2, projPoints3);

			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
					"3-view projected points sizes : %d %d %d %d %d %d\n",
					projPoints1.rows, projPoints1.cols, projPoints2.rows,
					projPoints2.cols, projPoints3.rows, projPoints3.cols);

			double cameraIdentity[3][4] = {{1,0,0,0},{0,1,0,0},{0,0,1,0}};
			double cameraIdentity2[3][4] = {{1,0,0,0},{0,1,0,0},{0,0,1,0}};
			cv::Mat cameraPos1 = cv::Mat(3, 4, CV_64FC1, &cameraIdentity),
					cameraPos2 = cv::Mat(3, 4, CV_64FC1, &cameraIdentity2); // Matrix 3x4


			// We have 3 cameras: 0, 1, 2
			// I think it should be made in coordinate system of camera (1)
			// Therefore, we express camera 0 in coordinates of camera 1
			Eigen::Matrix4f prev = VO::mat2eigen(prevRotation, prevTranslation);
			VO::eigen2cameraPos(prev.inverse(), cameraPos1);




			// Triangulation and scale estimation
			cv::Mat rotation23, translation23;
			cameraPos1 = cameraMatrix * cameraPos1;
			cameraPos2 = cameraMatrix * cameraPos2;



			estimateScale(projPoints1,projPoints2, projPoints3, cameraPos1, cameraPos2, rotation23, translation23,  cameraMatrix, distCoeffs);

			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC, "Det : %f \n",
					cv::determinant(rotation23));
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
					"Rot: %f %f %f\n", rotation23.at<double>(0, 0),
					rotation23.at<double>(1, 0), rotation23.at<double>(2, 0));
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
					"Rot: %f %f %f\n", rotation23.at<double>(0, 1),
					rotation23.at<double>(1, 1), rotation23.at<double>(2, 1));
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
					"Rot: %f %f %f\n", rotation23.at<double>(0, 2),
					rotation23.at<double>(1, 2), rotation23.at<double>(2, 2));
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
					"Scale : %f \n", VO::norm(translation23));

			// Saving current estimate
			Eigen::Matrix4f transformation = VO::mat2eigen(rotation23,
					translation23);
			transformation = transformation.inverse();
//			Eigen::Matrix4f transformation = VO::mat2eigen(rotation, translation);

			currentEstimate = currentEstimate * transformation;
			VO::eigen2matSave(currentEstimate, motionEstimate, i);
			scaleEstimate.at<double>(0,i) = VO::norm(translation23);

			// Copying for next iterations
			VO::eigen2mat(transformation, prevRotation, prevTranslation);

			inliers.copyTo(prevInliers);
		}
		// No scale
		else if ( ESTIMATION_TYPE == 2)
		{

			// Saving currect estimate
			Eigen::Matrix4f transformation = VO::mat2eigen(rotation, translation);

			currentEstimate = currentEstimate * transformation;
			VO::eigen2matSave(currentEstimate, motionEstimate, i);
			scaleEstimate.at<double>(0,i) = VO::norm(transformation);

			// Copying for next iterations
			VO::eigen2mat(transformation, prevRotation, prevTranslation);
		}
		// Let's copy stuff
		v[0].swap(v[1]);
		descriptors[1].copyTo(descriptors[0]);
		v[1].swap(v[2]);
		v[2].clear();
		descriptors[2].copyTo(descriptors[1]);
		matches.swap(prevMatches);
		matches.clear();
	}


	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_MSC,
				"Leaving native code\n");
}


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
	Mat inliers;

	gettimeofday(&start, NULL);
		FP::RotationTranslationFromFivePointAlgorithm(points2, points1, 1. / 500., numOfThreads, Npoint, rotation, translation, inliers, 525.0, 319.5, 235.5);
		//FP::RotationTranslationFromFivePointAlgorithm(points2, points1, 1. / 500., rotation, translation);
	gettimeofday(&end, NULL);


	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_DETDES,
								"Inliers size : %d %d \n", inliers.cols, inliers.rows);
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_DETDES,
									"Inliers : %d %d %d, %f %f %f \n", translation.cols, translation.rows, translation.type(),
									translation.at<double>(0,0), translation.at<double>(1,0), translation.at<double>(2,0));
		int ile = 0;
		for (int i=0;i<inliers.rows;i++)
		{
			if ( inliers.at<unsigned char>(i,0) == 1 )
			{
				ile ++ ;
			}
		}
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_DETDES,
							"Inliers : %d %d \n", ile, inliers.rows);


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


//



}

