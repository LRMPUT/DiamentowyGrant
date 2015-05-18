#include "DetectDescribe.h"

void DetectDescribe::performDetection(const cv::Mat& image,
		std::vector<cv::KeyPoint> & v, const int _detectorType) {
	cv::FeatureDetector *detector;

	switch (_detectorType) {
	case 1:
		detector = new cv::FastFeatureDetector();
		break;
	case 2:
		detector = new cv::StarFeatureDetector();
		break;
	case 3:
		detector = new cv::SiftFeatureDetector();
		break;
	case 4:
		detector = new cv::SurfFeatureDetector();
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
		detector = new cv::SimpleBlobDetector();
		break;
	case 10:
		detector = new cv::DenseFeatureDetector();
		break;
	default:
		detector = new cv::FastFeatureDetector();
	}
	detector->detect(image, v);
	delete detector;
}

void DetectDescribe::performDescription(const cv::Mat& image,
		std::vector<cv::KeyPoint> &v, cv::Mat & descriptors,
		const int _descriptorType) {
	DescriptorExtractor *extractor;
	LDB *ldb;
	cv::SiftDescriptorExtractor extractorSift;

	switch (_descriptorType) {
	case 0:
		ldb = new LDB();
		break;
	case 1:
		// SIFT is created statically
		break;
	case 2:
		extractor = new cv::SurfDescriptorExtractor();
		break;
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

	if (_descriptorType == 0) {
		cv::Mat dst;
		cv::cvtColor(image, dst, CV_BGR2GRAY);
		ldb->compute(dst, v, descriptors);
		delete ldb;
	} else if (_descriptorType == 1) {
		extractorSift.compute(image, v, descriptors);
	} else {
		extractor->compute(image, v, descriptors);
		delete extractor;
	}
}


void DetectDescribe::performMatching(const cv::Mat & descriptors, const cv::Mat & descriptors2, std::vector<cv::DMatch> &matches, int descriptorType)
{
	cv::BFMatcher *matcher;
	switch (descriptorType) {
	case 0:
	case 3:
	case 4:
	case 5:
		matcher = new cv::BFMatcher(NORM_HAMMING, true);
		break;
	case 1:
	case 2:
	case 6:
	default:
		matcher = new cv::BFMatcher(NORM_L2, true);
	}

	matcher->match(descriptors, descriptors2, matches);
	delete matcher;
}

int DetectDescribe::performParallelDetection(int _detectorType) {
	detectorType = _detectorType;

	// Starting N threads
	for (int i = 0; i < N; i++) {
		dataToProcess *data = new dataToProcess();
		data->classInstance = this;
		data->partToProcess = i;
		pthread_create(&thread_id[i], NULL,
				&DetectDescribe::parallelDetectionHelper, data);
	}

	// Waiting for N threads
	for (int j = 0; j < N; j++) {
		pthread_join(thread_id[j], NULL);
	}

	int sum = 0;
	for (int j = 0; j < N; j++) {
		sum += keypoints[j].size();
	}
	return sum;
}

void *DetectDescribe::pthreadParallelDetection(int part2Process) {

	struct timeval start;
	struct timeval end;
	gettimeofday(&start, NULL);

	DetectDescribe::performDetection(img[part2Process],keypoints[part2Process],detectorType);

	// Removing points that are in the border area
	int minY = borderEnlarger;
	if (part2Process == 0)
		minY = 0;
	int maxY = minY + 480/N;
	if (part2Process == N-1)
	{
		maxY +=1 ;
	}

	// erase - remove approach
	keypoints[part2Process].erase(
			std::remove_if(keypoints[part2Process].begin(),
					keypoints[part2Process].end(),
					removeIfOutsideBorder(minY, maxY)),
			keypoints[part2Process].end());

	gettimeofday(&end, NULL);

	int ret = ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)) / 1000;
//	__android_log_print(ANDROID_LOG_DEBUG,"DetectDescribe","Parallel detection time: %d\n", ret);

	return &keypoints[part2Process];
}

void DetectDescribe::performParallelDescription(int _param2) {
	descriptorType = _param2;

	// Starting N threads
	for (int i = 0; i < N; i++) {
		dataToProcess *data = new dataToProcess();
		data->classInstance = this;
		data->partToProcess = i;
		pthread_create(&thread_id[i], NULL,
				&DetectDescribe::parallelDescriptionHelper, data);
	}

	// Waiting for N threads
	for (int j = 0; j < N; j++) {
		pthread_join(thread_id[j], NULL);
	}
}

void *DetectDescribe::pthreadParallelDescription(int part2Process) {
	DetectDescribe::performDescription(img[part2Process], keypoints[part2Process], descriptors[part2Process], descriptorType);
}


void DetectDescribe::performParallelTracking() {
	// Starting N threads
	for (int i = 0; i < N; i++) {
		dataToProcess *data = new dataToProcess();
		data->classInstance = this;
		data->partToProcess = i;
		pthread_create(&thread_id[i], NULL,
				&DetectDescribe::parallelTrackingHelper, data);
	}

	// Waiting for N threads
	for (int j = 0; j < N; j++) {
		pthread_join(thread_id[j], NULL);
	}
}

void *DetectDescribe::pthreadParallelTracking(int part2Process) {

	std::vector<uchar> status;
	std::vector<float> err;
	cv::TermCriteria termcrit(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 15, 0.05);

	std::vector<cv::Point2f> points2Track, temp = std::vector<cv::Point2f>();
	cv::KeyPoint::convert(keypoints[part2Process], points2Track);

	// Calculating movement of features
	if ( points2Track.size() > 0)
	{
		cv::calcOpticalFlowPyrLK(img[part2Process], img2[part2Process],
				points2Track, temp, status, err, cvSize(5, 5), 3, termcrit);
	}
}



std::vector<cv::KeyPoint> DetectDescribe::getKeypointsFromParallelProcessing() {

	// Measuring total number of keypoints
	int totalSize = 0;
	for (int i=0;i<N;i++)
	{
		totalSize += keypoints[i].size();
	}

	// Reserving space for speed-up
	keypoints[0].reserve(totalSize);

	// Appending vectors
	for (int i=1;i<N;i++)
	{
		// Changing the coordinates to match those from single-threaded
		for (std::vector<cv::KeyPoint>::iterator iterFrom = keypoints[i].begin(); iterFrom != keypoints[i].end(); ++ iterFrom)
		{
			iterFrom->pt.y += ( i* 480/N - borderEnlarger);
		}

		// Intelligent copy
		keypoints[0].insert(keypoints[0].end(), keypoints[i].begin(), keypoints[i].end());
	}

	return keypoints[0];
}

cv::Mat DetectDescribe::getDescriptorsFromParallelProcessing()
{
	// Appending matrices for corresponding keypoints
	for (int i = 1; i < N; i++) {
		descriptors[0].push_back(descriptors[i]);
	}
	return descriptors[0];
}
