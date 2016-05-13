// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Jan Wietrzykowski & Michal Nowicki (michal.nowicki@put.poznan.pl)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>

#include <vector>
#include <string>
#include <fstream>
#include <android/log.h>

#include "DetectDescribe/DetectDescribe.h"
#include "openfabmap.hpp"

#define DEBUG_TAG_FABMAP "VisualPlaceRecognition_Fabmap"

extern "C" {

// Definitions
JNIEXPORT jlong JNICALL Java_org_dg_camera_VisualPlaceRecognition_createAndLoadFabmapNDK(JNIEnv* env,
		jobject thisObject, jstring settingsPath);

JNIEXPORT jlong JNICALL Java_org_dg_camera_VisualPlaceRecognition_createAndTrainFabmapNDK(JNIEnv* env,
		jobject thisObject, jstring settingsPath, jint trainingSetSize);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualPlaceRecognition_addTestSetFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmapEnv, jint testSetSize);

JNIEXPORT jint JNICALL Java_org_dg_camera_VisualPlaceRecognition_testLocationFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmapEnv, jlong addrTestImage, jboolean addToTest);

JNIEXPORT void JNICALL Java_org_dg_camera_VisualPlaceRecognition_destroyFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmapEnv);

std::string ConvertJString(JNIEnv* env, jstring str)
{
   if(!str){
	   return std::string();
   }

   const jsize len = env->GetStringUTFLength(str);
   const char* strChars = env->GetStringUTFChars(str, (jboolean *)0);

   std::string result(strChars, len);

   env->ReleaseStringUTFChars(str, strChars);

   return result;
}

void constructMatcherDetectorAndDescriptor(const cv::FileStorage& fsSettings,
								cv::Ptr<cv::DescriptorMatcher>& matcher,
									cv::Ptr<cv::FeatureDetector>& detector,
									cv::Ptr<cv::DescriptorExtractor>& extractor)
{
	std::string detectorType = fsSettings["FeatureOptions"]["DetectorType"];
	std::string extractorType = fsSettings["FeatureOptions"]["ExtractorType"];

	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
									"Creating matcher\n");

	matcher = cv::DescriptorMatcher::create("FlannBased");

	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
									"Creating detector, type = %s\n", detectorType.c_str());
	if(detectorType == "SIFT"){
		detector = new cv::SIFT(int(fsSettings["FeatureOptions"]["SiftDetector"]["NumFeatures"]),
								int(fsSettings["FeatureOptions"]["SiftDetector"]["NumOctaveLayers"]),
								double(fsSettings["FeatureOptions"]["SiftDetector"]["ContrastThreshold"]),
								double(fsSettings["FeatureOptions"]["SiftDetector"]["EdgeThreshold"]),
								double(fsSettings["FeatureOptions"]["SiftDetector"]["Sigma"]));
	}
	else if(detectorType == "SURF"){
		detector = new cv::SURF(double(fsSettings["FeatureOptions"]["SurfDetector"]["HessianThreshold"]),
								int(fsSettings["FeatureOptions"]["SurfDetector"]["NumOctaves"]),
								int(fsSettings["FeatureOptions"]["SurfDetector"]["NumOctaveLayers"]),
								(int)fsSettings["FeatureOptions"]["SurfDetector"]["Extended"] > 0,
								(int)fsSettings["FeatureOptions"]["SurfDetector"]["Upright"] > 0);
	}
	else{
		throw "Detector type not known";
	}

	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
									"Creating extractor, type = %s\n", extractorType.c_str());
	if(extractorType == "SIFT"){
		extractor = new cv::SIFT(int(fsSettings["FeatureOptions"]["SiftDetector"]["NumFeatures"]),
								int(fsSettings["FeatureOptions"]["SiftDetector"]["NumOctaveLayers"]),
								double(fsSettings["FeatureOptions"]["SiftDetector"]["ContrastThreshold"]),
								double(fsSettings["FeatureOptions"]["SiftDetector"]["EdgeThreshold"]),
								double(fsSettings["FeatureOptions"]["SiftDetector"]["Sigma"]));
	}
	else if(extractorType == "SURF"){
		extractor = new cv::SURF(double(fsSettings["FeatureOptions"]["SurfDetector"]["HessianThreshold"]),
								int(fsSettings["FeatureOptions"]["SurfDetector"]["NumOctaves"]),
								int(fsSettings["FeatureOptions"]["SurfDetector"]["NumOctaveLayers"]),
								(int)fsSettings["FeatureOptions"]["SurfDetector"]["Extended"] > 0,
								(int)fsSettings["FeatureOptions"]["SurfDetector"]["Upright"] > 0);
	}
	else{
		throw "Extractor type not known";
	}
}

void constructFabMap(const cv::FileStorage& fsSettings,
					cv::Mat trainData,
					cv::Mat clTree,
					of2::FabMap** fabmap)
{
	//create options flags
	std::string newPlaceMethod =
		fsSettings["openFabMapOptions"]["NewPlaceMethod"];
	std::string bayesMethod = fsSettings["openFabMapOptions"]["BayesMethod"];
	int simpleMotionModel = fsSettings["openFabMapOptions"]["SimpleMotion"];
	int options = 0;
	if(newPlaceMethod == "Sampled") {
		options |= of2::FabMap::SAMPLED;
	} else {
		options |= of2::FabMap::MEAN_FIELD;
	}
	if(bayesMethod == "ChowLiu") {
		options |= of2::FabMap::CHOW_LIU;
	} else {
		options |= of2::FabMap::NAIVE_BAYES;
	}
	if(simpleMotionModel) {
		options |= of2::FabMap::MOTION_MODEL;
	}

	*fabmap = new of2::FabMap2(clTree,
							double(fsSettings["openFabMapOptions"]["PzGe"]),
							double(fsSettings["openFabMapOptions"]["PzGne"]),
							options);


	//train FabMap for sampled new place likelihood approximation
	(*fabmap)->addTraining(trainData);
}

struct FabMapEnv{
	of2::FabMap *fabmap;
//	cv::Ptr<cv::DescriptorMatcher> matcher;
	cv::Ptr<cv::FeatureDetector> detector;
//	cv::Ptr<cv::DescriptorExtractor> extractor;
	cv::BOWImgDescriptorExtractor bide;
//	cv::Mat clTree;

	FabMapEnv(of2::FabMap *ifabmap,
			cv::Ptr<cv::FeatureDetector> idetector,
			cv::BOWImgDescriptorExtractor ibide) :
				fabmap(ifabmap),
				detector(idetector),
				bide(ibide)
			{}
};

// Declarations
JNIEXPORT jlong JNICALL Java_org_dg_camera_VisualPlaceRecognition_createAndLoadFabmapNDK(JNIEnv* env,
		jobject thisObject, jstring settingsPath)
{
	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
									"Called createFabmapNDK\n");

	std::string settingsPathStr = ConvertJString(env, settingsPath);

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
			"Successful accessed local vars\n");

	//get path to working dir
	std::size_t lastDir = settingsPathStr.rfind("/");
	std::string workingDir(settingsPathStr.begin(), settingsPathStr.begin() + lastDir);

	cv::FileStorage fsSettings;
	fsSettings.open(settingsPathStr, cv::FileStorage::READ);

	//load cached results
	cv::Mat vocab;
	std::string vocabPath = workingDir + "/" + string(fsSettings["FilePaths"]["Vocabulary"]);
	cv::FileStorage fsVocab(vocabPath, cv::FileStorage::READ);
	fsVocab["Vocabulary"] >> vocab;
	fsVocab.release();
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
				"vocab size = (%d, %d)\n", vocab.rows, vocab.cols);

	cv::Mat trainData;
	std::string trainDataPath = workingDir + "/" + string(fsSettings["FilePaths"]["TrainImagDesc"]);
	cv::FileStorage fsTrainData(trainDataPath, cv::FileStorage::READ);
	fsTrainData["BOWImageDescs"] >> trainData;
	fsTrainData.release();
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
					"trainData size = (%d, %d)\n", trainData.rows, trainData.cols);

	cv::Mat clTree;
	std::string clTreePath = workingDir + "/" + string(fsSettings["FilePaths"]["ChowLiuTree"]);
	cv::FileStorage fsClTree(clTreePath, cv::FileStorage::READ);
	fsClTree["ChowLiuTree"] >> clTree;
	fsClTree.release();

	//construct matcher, detector and extractor
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
			"Constructing match, det and extr\n");
	cv::Ptr<cv::DescriptorMatcher> matcher = NULL;
	cv::Ptr<cv::FeatureDetector> detector = NULL;
	cv::Ptr<cv::DescriptorExtractor> extractor = NULL;

	constructMatcherDetectorAndDescriptor(fsSettings,
										matcher,
										detector,
										extractor);

	//create BOW image descriptor and extractor
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
			"Constructing BIDE\n");
	cv::BOWImgDescriptorExtractor bide(extractor, matcher);
	bide.setVocabulary(vocab);

	//construct FabMap2
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
			"Constructing FabMap\n");
	of2::FabMap* fabmap;

	constructFabMap(fsSettings,
					trainData,
					clTree,
					&fabmap);

	//create FabMapEnv
	FabMapEnv* fabMapEnv = new FabMapEnv(fabmap,
											detector,
											bide);

	// We return the address of the instance
	return (long) fabMapEnv;
}


JNIEXPORT jlong JNICALL Java_org_dg_camera_VisualPlaceRecognition_createAndTrainFabmapNDK(JNIEnv* env,
		jobject thisObject, jstring settingsPath, jint trainingSetSize)
{
	try{
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Called createAndTrainFabmapNDK\n");

		// Find the required classes
		jclass thisclass = env->GetObjectClass(thisObject);
		jclass matclass = env->FindClass("org/opencv/core/Mat");

		// Get methods and fields
		jmethodID getPtrMethod = env->GetMethodID(matclass, "getNativeObjAddr",
												"()J");
		jfieldID bufimgsfieldid = env->GetFieldID(thisclass, "trainImages",
												"[Lorg/opencv/core/Mat;");

		// Let's start: Get the fields
		jobjectArray bufimgsArray = (jobjectArray) env->GetObjectField(thisObject,
						bufimgsfieldid);

		// Convert the array
		cv::Mat trainImages[trainingSetSize];
		for (int i = 0; i < trainingSetSize; i++)
		{
			trainImages[i] = *(cv::Mat*) env->CallLongMethod(
					env->GetObjectArrayElement(bufimgsArray, i), getPtrMethod);
		}

		std::string settingsPathStr = ConvertJString(env, settingsPath);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
				"Successful accessed local vars\n");

		cv::FileStorage fsSettings;
		fsSettings.open(settingsPathStr, cv::FileStorage::READ);

	//	std::string detectorType = fsSettings["FeatureOptions"]["DetectorType"];
	//	std::string extractorType = fsSettings["FeatureOptions"]["ExtractorType"];

		//construct matcher, detector and extractor
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Constructing match, desc and extr\n");
		cv::Ptr<cv::DescriptorMatcher> matcher = NULL;
		cv::Ptr<cv::FeatureDetector> detector = NULL;
		cv::Ptr<cv::DescriptorExtractor> extractor = NULL;

		constructMatcherDetectorAndDescriptor(fsSettings,
											matcher,
											detector,
											extractor);

		//extract features
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Extracting features\n");

		cv::Mat trainDesc;
		for (int i = 0; i < trainingSetSize; i++)
		{
			std::vector<cv::KeyPoint> kpts;
			cv::Mat curDesc;

	//		if(detectorType == "SIFT"){
	//			DetectDescribe::performDetection(trainingImages[i], kpts, 3);
	//		}
	//		else if(detectorType == "SURF"){
	//			DetectDescribe::performDetection(trainingImages[i], kpts, 4);
	//		}
	//		else{
	//			throw "Detector type not known";
	//		}
	//
	//		if(extractorType == "SIFT"){
	//			DetectDescribe::performDescription(trainingImages[i], kpts, curDesc, 1);
	//		}
	//		else if(extractorType == "SURF"){
	//			DetectDescribe::performDescription(trainingImages[i], kpts, curDesc, 2);
	//		}
	//		else{
	//			throw "Extractor type not known";
	//		}

			__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
					"image %d, size =(%d, %d)\n", i, trainImages[i].cols, trainImages[i].rows);

			detector->detect(trainImages[i], kpts);
			extractor->compute(trainImages[i], kpts, curDesc);

			__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
					"image %d, kpts.size() = %d\n", i, kpts.size());

			trainDesc.push_back(curDesc);
		}


	//	std::string trainPath = fsSettings["FilePaths"]["TrainPath"];
	//	std::ifstream trainFilenames(trainPath);
	//
		//get path to working dir
		std::size_t lastDir = settingsPathStr.rfind("/");
		std::string workingDir(settingsPathStr.begin(), settingsPathStr.begin() + lastDir);
	//
	//	while(!trainFilenames.eof() && ! trainFilenames.fail()){
	//		std::string curName;
	//	}

		//train vocabulary
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Training vocabulary\n");
		double clusterRadius = fsSettings["VocabTrainOptions"]["ClusterSize"];
		//uses Modified Sequential Clustering to train a vocabulary
		of2::BOWMSCTrainer trainer(clusterRadius);
		trainer.add(trainDesc);
		cv::Mat vocab = trainer.cluster();
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"vocabulary size = (%d, %d)\n", vocab.cols, vocab.rows);

		//extract words
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Extracting words\n");

		cv::BOWImgDescriptorExtractor bide(extractor, matcher);
		bide.setVocabulary(vocab);

		cv::Mat trainData;
		for (int i = 0; i < trainingSetSize; i++)
		{
			cv::Mat bow;
			std::vector<cv::KeyPoint> kpts;

			detector->detect(trainImages[i], kpts);
			bide.compute(trainImages[i], kpts, bow);

			trainData.push_back(bow);
		}

		//train Chow-Liu Tree
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Training Chow-Liu Tree\n");
		double lowerInformationBound = fsSettings["ChowLiuOptions"]["LowerInfoBound"];
		of2::ChowLiuTree tree;
		tree.add(trainData);
		cv::Mat clTree = tree.make(lowerInformationBound);

		//construct FabMap2
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Constructing FabMap\n");
		of2::FabMap* fabmap;

		constructFabMap(fsSettings,
						trainData,
						clTree,
						&fabmap);

		//save results
		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Saving results\n");
		std::string vocabPath = workingDir + "/" + string(fsSettings["FilePaths"]["Vocabulary"]);
		cv::FileStorage fsVocab(vocabPath, cv::FileStorage::WRITE);
		fsVocab << "Vocabulary" << vocab;
		fsVocab.release();

		std::string trainDataPath = workingDir + "/" + string(fsSettings["FilePaths"]["TrainImagDesc"]);
		cv::FileStorage fsTrainData(trainDataPath, cv::FileStorage::WRITE);
		fsTrainData << "BOWImageDescs" << trainData;
		fsTrainData.release();

		std::string clTreePath = workingDir + "/" + string(fsSettings["FilePaths"]["ChowLiuTree"]);
		cv::FileStorage fsClTree(clTreePath, cv::FileStorage::WRITE);
		fsClTree << "ChowLiuTree" << clTree;
		fsClTree.release();

		FabMapEnv* fabMapEnv = new FabMapEnv(fabmap,
												detector,
												bide);

		return (long) fabMapEnv;
	}
	catch(char const* error){
		__android_log_print(ANDROID_LOG_DEBUG,
							DEBUG_TAG_FABMAP,
							"Char exception in CATF: %s \n", error);
		throw;
//		cout << "Char exception in main: " << error << endl;
	}
	catch(std::exception& e){
		__android_log_print(ANDROID_LOG_DEBUG,
							DEBUG_TAG_FABMAP,
							"Std exception in CATF: %s \n", e.what());
		throw;
//		cout << "Std exception in main: " << e.what() << endl;
	}
	catch(...){
		__android_log_print(ANDROID_LOG_DEBUG,
							DEBUG_TAG_FABMAP,
							"Unexpected exception in CATF\n");
		throw;
//		cout << "Unexpected exception in main" << endl;
	}
}

JNIEXPORT void JNICALL Java_org_dg_camera_VisualPlaceRecognition_addTestSetFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmapEnv, jint testSetSize)
{
	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
									"Called addTestSetFabmapNDK\n");

	// Find the required classes
	jclass thisclass = env->GetObjectClass(thisObject);
	jclass matclass = env->FindClass("org/opencv/core/Mat");

	// Get methods and fields
	jmethodID getPtrMethod = env->GetMethodID(matclass, "getNativeObjAddr",
											"()J");
	jfieldID bufimgsfieldid = env->GetFieldID(thisclass, "testImages",
											"[Lorg/opencv/core/Mat;");

	// Let's start: Get the fields
	jobjectArray bufimgsArray = (jobjectArray) env->GetObjectField(thisObject,
					bufimgsfieldid);

	// Convert the array
	cv::Mat testImages[testSetSize];
	for (int i = 0; i < testSetSize; i++)
	{
		testImages[i] = *(cv::Mat*) env->CallLongMethod(
				env->GetObjectArrayElement(bufimgsArray, i), getPtrMethod);
	}

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
			"Successful accessed local vars\n");

	FabMapEnv* fabMapEnv = (FabMapEnv*)addrFabmapEnv;

	cv::Mat testData;
	for (int i = 0; i < testSetSize; i++)
	{
		cv::Mat bow;
		std::vector<cv::KeyPoint> kpts;

		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
						"image %d, size =(%d, %d)\n", i, testImages[i].cols, testImages[i].rows);

		fabMapEnv->detector->detect(testImages[i], kpts);
		fabMapEnv->bide.compute(testImages[i], kpts, bow);

		__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
				"image %d, kpts.size() = %d\n", i, kpts.size());

		testData.push_back(bow);
	}

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
			"Adding to test set, testData.size() = (%d, %d)\n", testData.cols, testData.rows);

	fabMapEnv->fabmap->add(testData);
}


JNIEXPORT jint JNICALL Java_org_dg_camera_VisualPlaceRecognition_testLocationFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmapEnv, jlong addrTestImage, jboolean addToTest)
{
	__android_log_print(ANDROID_LOG_DEBUG,  DEBUG_TAG_FABMAP,
										"Called testLocationNDK\n");

	static int count = 0;
	static float detSum = 0.0;
	static float descSum = 0.0;
	static float compSum = 0.0;

	timeval start;
	timeval endDet;
	timeval endDesc;
	timeval endComp;


	gettimeofday(&start, NULL);


	// The image to test location
	cv::Mat& testImage = *(cv::Mat*) addrTestImage;

	FabMapEnv* fabMapEnv = (FabMapEnv*)addrFabmapEnv;

	cv::Mat bow;
	std::vector<cv::KeyPoint> kpts;

	fabMapEnv->detector->detect(testImage, kpts);

	gettimeofday(&endDet, NULL);

	fabMapEnv->bide.compute(testImage, kpts, bow);

	gettimeofday(&endDesc, NULL);

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
					"kpts.size() = %d\n", kpts.size());

	//to avoid mismatch - 100 is a safe value
	static const int kptsThresh = 100;
	if(kpts.size() < kptsThresh){
		return -1;
	}

	std::vector<of2::IMatch> matches;
	if(!bow.empty()){
		fabMapEnv->fabmap->compare(bow, matches, addToTest);
	}

	gettimeofday(&endComp, NULL);

	++count;
	detSum += ((endDet.tv_sec * 1000000 + endDet.tv_usec)
			- (start.tv_sec * 1000000 + start.tv_usec)) / 1000.0;
	descSum += ((endDesc.tv_sec * 1000000 + endDesc.tv_usec)
				- (endDet.tv_sec * 1000000 + endDet.tv_usec)) / 1000.0;
	compSum += ((endComp.tv_sec * 1000000 + endComp.tv_usec)
					- (endDesc.tv_sec * 1000000 + endDesc.tv_usec)) / 1000.0;

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
						"mean detection time = %f ms\n", detSum/count);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
						"mean description time = %f ms\n", descSum/count);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
						"mean comparison time = %f ms\n", compSum/count);

//	static const double matchThresh = 0.5;
	double bestMatchProb = 0.0;
	int bestMatchTestIdx = -1;
	for(std::vector<of2::IMatch>::iterator it = matches.begin(); it != matches.end(); ++it){
//		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
//				"testIdx = %d, match = %f\n", it->imgIdx, it->match);
//		if(it->match > matchThresh){
			if(bestMatchProb < it->match){
				bestMatchProb = it->match;
				bestMatchTestIdx = it->imgIdx;
			}
//		}
	}

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_FABMAP,
					"bestMatchProb = %f\n", bestMatchProb);

	// Return the id of the recognized place, -1 if it is different place than the images in the database
	return bestMatchTestIdx;
}

JNIEXPORT void JNICALL Java_org_dg_camera_VisualPlaceRecognition_destroyFabmapNDK(JNIEnv* env,
		jobject thisObject, jlong addrFabmapEnv)
{
	FabMapEnv* fabMapEnv = (FabMapEnv*)addrFabmapEnv;

	delete fabMapEnv->fabmap;
	delete fabMapEnv;
}

}
