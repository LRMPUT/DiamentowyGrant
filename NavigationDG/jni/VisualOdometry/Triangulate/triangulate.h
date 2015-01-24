#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/calib3d/calib3d.hpp>


#include <Eigen/Dense>

// PNP type:
// - CV_ITERATIVE
// - CV_P3P
// - CV_EPNP
void estimateScale(cv::Mat projPoints1, cv::Mat projPoints2,
		cv::Mat projPoints3, cv::Mat cameraPos1, cv::Mat cameraPos2,
		cv::Mat &rotation, cv::Mat &translation, cv::Mat cameraMatrix, cv::Mat distCoeffs, int flag = CV_ITERATIVE);


// Estimates scale on translations: (t12 can have scale included or given as next parameter)
//       t23
//       ___
//      \   |
//	 t23 \  |  t12
//        \ |
//	       \|
double estimateScaleTriangle(Eigen::Vector3d trans12, Eigen::Vector3d trans13, Eigen::Vector3d trans23, double scale12 = 1);
double estimateScaleTriangle(cv::Mat trans12, cv::Mat trans13, cv::Mat trans23, double scale12 = 1);
