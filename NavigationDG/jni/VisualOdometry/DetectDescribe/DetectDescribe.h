#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>


#include "NonFree/nonfree/features2d.hpp"
#include "NonFree/nonfree/nonfree.hpp"


#include "NonFree/nonfree/features2d.hpp"
#include "NonFree/nonfree/nonfree.hpp"
#include "LDB/ldb.h"


#include <vector>
#include <android/log.h>


class removeIfOutsideBorder{
	int startY, endY;

public:
    removeIfOutsideBorder(int _startY, int _endY)
    {
    	startY = _startY;
    	endY = _endY;
    };

    bool operator()(cv::KeyPoint kp) const {
        if (kp.pt.y < startY || kp.pt.y >= endY) {
            return true;
        }
        return false;
    }

};


class DetectDescribe {
public:

	struct dataToProcess {
	    DetectDescribe* classInstance;
	    int partToProcess;
	};


	int detectorType, N, descriptorType;

	// Parallel data
	cv::Mat img[6], image, img2[6], image2;
	std::vector<cv::KeyPoint> keypoints[6];
	int startY[6], endY[6], borderEnlarger;
	cv::Mat descriptors[6];
	pthread_t thread_id[6];



	DetectDescribe(cv::Mat inputImage, int numOfThreads, int borderEnlarge = 20) {
		N = numOfThreads;
		detectorType = 0;
		descriptorType = 0;
		borderEnlarger = borderEnlarge;
		image = inputImage.clone();

		for (int i=0;i<N;i++)
		{
			int startX = 0;
			startY[i] = 480/N * i;
			int endX = 640;
			endY[i] = 480/N;


//			__android_log_print(ANDROID_LOG_DEBUG, "DetectDescribe", "Image size: %d %d\n", image.cols, image.rows);

			if (i == 0) {
				img[i] = image(
						cv::Rect(startX, 0, endX, endY[i] + borderEnlarger));
			}
			if (i + 1 < N) {
				img[i] = image(
						cv::Rect(startX, max(0, startY[i] - borderEnlarger),
								endX, endY[i] + 2 * borderEnlarger));
			} else {
				img[i] = image(
						cv::Rect(startX, startY[i] - borderEnlarger, endX,
								endY[i] + borderEnlarger));
			}
		}
	}

	DetectDescribe(cv::Mat inputImage, cv::Mat inputImage2, int numOfThreads, int borderEnlarge = 20) {
			N = numOfThreads;
			detectorType = 0;
			descriptorType = 0;
			borderEnlarger = borderEnlarge;
			image = inputImage.clone();
			image2 = inputImage2.clone();

			for (int i=0;i<N;i++)
			{
				int startX = 0;
				startY[i] = 480/N * i;
				int endX = 640;
				endY[i] = 480/N;


	//			__android_log_print(ANDROID_LOG_DEBUG, "DetectDescribe", "Image size: %d %d\n", image.cols, image.rows);

				if (i == 0) {
					img[i] = image(
							cv::Rect(startX, 0, endX, endY[i] + borderEnlarger));
					img2[i] = image2(
												cv::Rect(startX, 0, endX, endY[i] + borderEnlarger));
				}
				if (i + 1 < N) {
					img[i] = image(
							cv::Rect(startX, max(0, startY[i] - borderEnlarger),
									endX, endY[i] + 2 * borderEnlarger));
					img2[i] = image2(
												cv::Rect(startX, max(0, startY[i] - borderEnlarger),
														endX, endY[i] + 2 * borderEnlarger));
				} else {
					img[i] = image(
							cv::Rect(startX, startY[i] - borderEnlarger, endX,
									endY[i] + borderEnlarger));
					img2[i] = image2(
												cv::Rect(startX, startY[i] - borderEnlarger, endX,
														endY[i] + borderEnlarger));
				}
			}
		}

	~DetectDescribe()
	{
		image.release();
		for (int i=0;i<N;i++)
		{
			img[i].release();
			descriptors[i].release();
		}
	}


	static void performDetection(const cv::Mat& image,
			std::vector<cv::KeyPoint> & v, const int _detectorType);
	static void performDescription(const cv::Mat& image,
			std::vector<cv::KeyPoint> &v, cv::Mat & descriptors,
			const int _descriptorType);

	static void performMatching(const cv::Mat & descriptors, const cv::Mat & descriptors2, std::vector<cv::DMatch> &matches, int descriptorType);

	int performParallelDetection(int _detectorType);
	void performParallelDescription(int _descriptorType);

	void performParallelTracking();

	std::vector<cv::KeyPoint> getKeypointsFromParallelProcessing();
	cv::Mat getDescriptorsFromParallelProcessing();

private:

	void *pthreadParallelDetection(int part2Process);
	void *pthreadParallelDescription(int part2Process);

	void *pthreadParallelTracking(int part2Process);

	static void *parallelDetectionHelper(void *pointerToClass) {
		dataToProcess &data = *(dataToProcess*) pointerToClass;
		void * ret = ((DetectDescribe *) data.classInstance)->pthreadParallelDetection(data.partToProcess);
		delete &data;
		return ret;
	}

	static void *parallelDescriptionHelper(void *pointerToClass) {
		dataToProcess &data = *(dataToProcess*) pointerToClass;
		void * ret = ((DetectDescribe *) data.classInstance)->pthreadParallelDescription(data.partToProcess);
		delete &data;
		return ret;
	}

	static void *parallelTrackingHelper(void *pointerToClass) {
			dataToProcess &data = *(dataToProcess*) pointerToClass;
			void * ret = ((DetectDescribe *) data.classInstance)->pthreadParallelTracking(data.partToProcess);
			delete &data;
			return ret;
		}


};

