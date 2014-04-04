#ifndef VISUALODOMETRY_H
#define VISUALODOMETRY_H
#include <vector>
#include <opencv2/core/core.hpp>
using namespace cv;

class VisualOdometry
{
public:
	VisualOdometry();

	static void rotationToQuaternion(Mat& rotation, Mat& quaternion);
	static void quaternionToRotation(Mat& quaternion, Mat& rotation);
	static int  findRotationAndTranslation(std::vector<Point2f> &points1, std::vector<Point2f> &points2, Mat &R, Mat &t, double error);
};

#endif // VISUALODOMETRY_H
