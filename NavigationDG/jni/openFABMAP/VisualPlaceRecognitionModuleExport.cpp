#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>

#include <vector>
#include <android/log.h>

#include "openfabmap.hpp"

#define DEBUG_TAG_FABMAP "VisualPlaceRecognition_Fabmap"

extern "C" {

// Definitions
JNIEXPORT jlong JNICALL Java_org_dg_camera_VisualPlaceRecognition_createFabmapNDK(JNIEnv* env,
		jobject thisObject, jstring settingsPath);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualPlaceRecognition_trainNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmap, jint trainingSetSize);

JNIEXPORT jint JNICALL Java_org_dg_camera_VisualPlaceRecognition_testLocationNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmap, jlong addrImgToTest);

JNIEXPORT jint JNICALL Java_org_dg_camera_VisualPlaceRecognition_destroyFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmap);


// Declarations
JNIEXPORT jlong JNICALL Java_org_dg_camera_VisualPlaceRecognition_createFabmapNDK(JNIEnv* env,
		jobject thisObject, jstring settingsPath)
{
	// This is only a mock-up of creation. It should be done as in openFABMAPcli.cpp in generateFABMAPInstance
	// Use settingsPath to provide file with already stored settings
	cv::Mat clTree = cv::Mat::ones(3,3, CV_64FC1);
	of2::FabMap *fabmap; // Nie uda³o mi siê stworzyæ mock-upu: new of2::FabMap2(clTree,3,3,6);

	// We return the address of the instance
	return (long) fabmap;


}


JNIEXPORT void JNICALL Java_org_dg_camera_VisualPlaceRecognition_trainNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmap, jint trainingSetSize)
{
	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
									"Called trainNDK\n");

	// Extract Fabmap class
	// Uncomment when fabmap is created: of2::FabMap &fabmap = *(of2::FabMap*) addrFabmap;

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);
	jclass matclass = env->FindClass("org/opencv/core/Mat");

	// Get methods and fields
	jmethodID getPtrMethod = env->GetMethodID(matclass, "getNativeObjAddr",
					"()J");
	jfieldID bufimgsfieldid = env->GetFieldID(thisclass, "trainingImages",
					"[Lorg/opencv/core/Mat;");

	// Let's start: Get the fields
	jobjectArray bufimgsArray = (jobjectArray) env->GetObjectField(thisObject,
					bufimgsfieldid);

	// Convert the array
	cv::Mat trainingImages[trainingSetSize];
	for (int i = 0; i < trainingSetSize; i++)
	{
		trainingImages[i] = *(cv::Mat*) env->CallLongMethod(
				env->GetObjectArrayElement(bufimgsArray, i), getPtrMethod);
	}

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
			"Successful accessed local vars\n");


	//
	// Do as you wish with variables:
	// - fabmap
	// - trainingImages
	// - trainingSetSize
	//
}


JNIEXPORT jint JNICALL Java_org_dg_camera_VisualPlaceRecognition_testLocationNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmap, jlong addrImgToTest)
{
	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Called testLocationNDK\n");

	// Extract Fabmap class
	// Uncomment when fabmap is created: of2::FabMap &fabmap = *(of2::FabMap*) addrFabmap;

	// The image to test location
	cv::Mat& imgToTest = *(cv::Mat*) addrImgToTest;

	//
	// Do as you wish with variables:
	// - fabmap
	// - imgToTest
	//

	// Return the id of the recognized place, -1 if it is different place than the images in the database
	return -1;
}

JNIEXPORT jint JNICALL Java_org_dg_camera_VisualPlaceRecognition_destroyFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmap)
{
	// Destroy object
	delete (of2::FabMap*) (addrFabmap);
}

}
