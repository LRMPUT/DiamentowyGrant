#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include "nonfree/features2d.hpp"
#include "nonfree/nonfree.hpp"
#include <vector>

using namespace std;
using namespace cv;

extern "C" {

// Export declarations
JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SurfFeatures(JNIEnv*, jobject, jlong addrGray, jint param);
JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SurfDescription(JNIEnv*, jobject, jlong addrGray, jint param);
JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SiftFeatures(JNIEnv*, jobject, jlong addrGray, jint param);
JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SiftDescription(JNIEnv*, jobject, jlong addrGray, jint param);

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SurfFeatures(JNIEnv*, jobject, jlong addrGray, jint param)
{

    Mat& mGr  = *(Mat*)addrGray;
    vector<KeyPoint> v;


    struct timeval start;
	struct timeval end;

	gettimeofday(&start, NULL);

	SurfFeatureDetector detector;

	detector.detect(mGr, v);

	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	return ret;

}

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SurfDescription(JNIEnv*, jobject, jlong addrGray, jint param)
{
    Mat& mGr  = *(Mat*)addrGray;
    vector<KeyPoint> v;


    struct timeval start;
	struct timeval end;

	SurfFeatureDetector detector;

	detector.detect(mGr, v);


	SurfDescriptorExtractor extractor;
	Mat descriptors_object;



	gettimeofday(&start, NULL);

	extractor.compute( mGr, v, descriptors_object );

	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	return ret;

}



JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SiftFeatures(JNIEnv*, jobject, jlong addrGray, jint param)
{
    Mat& mGr  = *(Mat*)addrGray;
    vector<KeyPoint> v;


    struct timeval start;
	struct timeval end;

	gettimeofday(&start, NULL);

	SiftFeatureDetector detector;

	detector.detect(mGr, v);

	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	return ret;
}

JNIEXPORT int JNICALL Java_org_dg_main_VisualOdometry_SiftDescription(JNIEnv*, jobject, jlong addrGray, jint param)
{
    Mat& mGr  = *(Mat*)addrGray;
    vector<KeyPoint> v;


    struct timeval start;
	struct timeval end;

	SiftFeatureDetector detector;

	detector.detect(mGr, v);


	SiftDescriptorExtractor extractor;
	Mat descriptors_object;



	gettimeofday(&start, NULL);

	extractor.compute( mGr, v, descriptors_object );

	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000;

	return ret;

}

}
