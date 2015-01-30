#ifndef __MY_FIVE_POINT_H__
#define __MY_FIVE_POINT_H__

#include <opencv2/core/core.hpp>


namespace FP // FivePoint
{
void RotationTranslationFromFivePointAlgorithm(const cv::Mat& points1,
		const cv::Mat& points2, double threshold, int numOfThreads, int Npoint,
		cv::Mat& rotation, cv::Mat& translation, cv::Mat &essentialMatrixInliers, double focal, double cx, double cy);
}

#endif

