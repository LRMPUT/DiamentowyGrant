#ifndef __MY_FIVE_POINT_H__
#define __MY_FIVE_POINT_H__

#include <opencv2/core/core.hpp>
#include <omp.h>

namespace FP // FivePoint
{
	void RotationTranslationFromFivePointAlgorithm(const cv::Mat& points1, const cv::Mat& points2, double threshold, cv::Mat& rotation, cv::Mat& translation);
}

#endif

