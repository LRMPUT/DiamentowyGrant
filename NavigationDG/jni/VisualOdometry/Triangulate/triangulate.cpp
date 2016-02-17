// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Michal Nowicki (michal.nowicki@put.poznan.pl)
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

#include "triangulate.h"
#include <android/log.h>

void estimateScale(cv::Mat projPoints1, cv::Mat projPoints2,
		cv::Mat projPoints3, cv::Mat cameraPos1, cv::Mat cameraPos2,
		cv::Mat &rotation, cv::Mat &translation, cv::Mat cameraMatrix, cv::Mat distCoeffs, int flag) {
	cv::Mat points3D;

	for (int k = 0; k < 3; k++)
					__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
							"CameraPos1: %f %f %f %f\n", cameraPos1.at<double>(k,0), cameraPos1.at<double>(k,1), cameraPos1.at<double>(k,2), cameraPos1.at<double>(k,3));

	for (int k = 0; k < 3; k++)
					__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
							"CameraPos2: %f %f %f %f\n", cameraPos2.at<double>(k,0), cameraPos2.at<double>(k,1), cameraPos2.at<double>(k,2), cameraPos2.at<double>(k,3));

	// Testing coordinate systems
//	cv::Mat p3D = cv::Mat(4,1, CV_64F);
//	p3D.at<double>(0,0) = 0;
//	p3D.at<double>(1,0) = 0;
//	p3D.at<double>(2,0) = -1000;
//	p3D.at<double>(3,0) = 1;
//
	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis", "xxx");

//	cv::Mat x = cameraPos1 * p3D;
//	cv::Mat y = cameraPos2 * p3D;

	cv::Mat x = cv::Mat(3,1, CV_64F), y = cv::Mat(3,1, CV_64F);
	x.at<double>(0,0) = projPoints1.at<cv::Vec2f>(0,0)[0];
	x.at<double>(1,0) = projPoints1.at<cv::Vec2f>(0,0)[1];
	x.at<double>(2,0) = 1.0;

	y.at<double>(0,0) = projPoints2.at<cv::Vec2f>(0,0)[0];
	y.at<double>(1,0) = projPoints2.at<cv::Vec2f>(0,0)[1];
	y.at<double>(2,0) = 1.0;


	cv::Mat px = cv::Mat(2,1, CV_64F), py = cv::Mat(2,1, CV_64F);
	px.at<double>(0,0) = x.at<double>(0,0) / x.at<double>(2,0);
	px.at<double>(1,0) = x.at<double>(1,0) / x.at<double>(2,0);
	py.at<double>(0,0) = y.at<double>(0,0) / y.at<double>(2,0);
	py.at<double>(1,0) = y.at<double>(1,0) / y.at<double>(2,0);

	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis", "xy");

	cv::triangulatePoints(cameraPos1, cameraPos2, px, py, points3D);

	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis", "yyy");

	std::vector<cv::Point3f> tmp2;
	for (int i=0;i<points3D.cols;i++)
	{
		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
							"test - 3D point: %f %f %f %f\n", points3D.at<double>(0,i), points3D.at<double>(1,i), points3D.at<double>(2,i), points3D.at<double>(3,i));
		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
							"test - 3D point: %f %f %f\n", points3D.at<double>(0,i)/points3D.at<double>(3,i), points3D.at<double>(1,i)/points3D.at<double>(3,i), points3D.at<double>(2,i)/points3D.at<double>(3,i));

		cv::Mat temporal  = cv::Mat(4,1, CV_64F);
		temporal.at<double>(0,0) = points3D.at<double>(0,i);
		temporal.at<double>(1,0) = points3D.at<double>(1,i);
		temporal.at<double>(2,0) = points3D.at<double>(2,i);
		temporal.at<double>(3,0) = points3D.at<double>(3,i);
		cv::Mat projected = cameraPos1 * temporal;
		projected.at<double>(0) = projected.at<double>(0) / projected.at<double>(2);
		projected.at<double>(1) = projected.at<double>(1) / projected.at<double>(2);
		cv::Mat projected2 = cameraPos2 * temporal;
				projected2.at<double>(0) = projected2.at<double>(0) / projected2.at<double>(2);
				projected2.at<double>(1) = projected2.at<double>(1) / projected2.at<double>(2);
		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
						"Projection compare:  %f %f | %f %f\n",
						projected.at<double>(0), projected.at<double>(1), px.at<double>(0,0), px.at<double>(1,0));
		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
								"Projection2 compare:  %f %f | %f %f\n",
								projected2.at<double>(0), projected2.at<double>(1), py.at<double>(0,0), py.at<double>(1,0));
	}



	// 3D positions of inliers
	cv::triangulatePoints(cameraPos1, cameraPos2, projPoints1, projPoints2, points3D);

	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
			"3D size: %d %d %d\n", points3D.rows, points3D.cols, points3D.type());

	std::vector<cv::Point3f> tmp;
	//points3D.convertTo(point)
//	for (int i=0;i<points3D.cols;i++)
//	{
////		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
////									"3D point on images: %f %f %f %f\n", projPoints1.at<cv::Vec2f>(i)[0], projPoints1.at<cv::Vec2f>(i)[1], projPoints2.at<cv::Vec2f>(i)[0], projPoints2.at<cv::Vec2f>(i)[1]  );
////
////
////		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
////							"3D point: %f %f %f %f\n", points3D.at<float>(0,i), points3D.at<float>(1,i), points3D.at<float>(2,i), points3D.at<float>(3,i));
//		tmp.push_back(cv::Point3f(points3D.at<float>(0,i)/points3D.at<float>(3,i), points3D.at<float>(1,i)/points3D.at<float>(3,i),points3D.at<float>(2,i)/points3D.at<float>(3,i)));
//	}



	// Projection test
	std::vector<cv::Point2f> projectionOn3;
	for (int i=0;i<points3D.cols;i++)
	{
		cv::Mat temporal  = cv::Mat(4,1, CV_64F);
		temporal.at<double>(0,0) = points3D.at<float>(0,i)/points3D.at<float>(3,i);
		temporal.at<double>(1,0) = points3D.at<float>(1,i)/points3D.at<float>(3,i);
		temporal.at<double>(2,0) = points3D.at<float>(2,i)/points3D.at<float>(3,i);
		temporal.at<double>(3,0) = 1.0;

		cv::Mat projected = cameraPos1 * temporal;
		projected.convertTo(projected, CV_64F);
		projected.at<double>(0) = projected.at<double>(0,0) / projected.at<double>(2,0);
		projected.at<double>(1) = projected.at<double>(1,0) / projected.at<double>(2,0);
//		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
//				"Projection compare: %f %f vs %f %f \n",
//				projected.at<double>(0), projected.at<double>(1), projPoints1.at<cv::Vec2f>(i,0)[0], projPoints1.at<cv::Vec2f>(i,0)[1]);


		cv::Mat projected2 = cameraPos2 * temporal;
		projected2.convertTo(projected2, CV_64F);
		projected2.at<double>(0) = projected2.at<double>(0,0) / projected2.at<double>(2,0);
		projected2.at<double>(1) = projected2.at<double>(1,0) / projected2.at<double>(2,0);
//		__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
//				"Projection compare: %f %f vs %f %f \n",
//				projected2.at<double>(0), projected2.at<double>(1), projPoints2.at<cv::Vec2f>(i,0)[0], projPoints2.at<cv::Vec2f>(i,0)[1]);

		double error = pow(projected.at<double>(0) - projPoints1.at<cv::Vec2f>(i,0)[0], 2) + pow( projected.at<double>(1) -  projPoints1.at<cv::Vec2f>(i,0)[1], 2);
		double error2 = pow(projected2.at<double>(0) - projPoints2.at<cv::Vec2f>(i,0)[0], 2) + pow( projected2.at<double>(1) -  projPoints2.at<cv::Vec2f>(i,0)[1], 2);

		if ( error < 4.0 && error2 < 4.0)
		{
			tmp.push_back(cv::Point3f(points3D.at<float>(0,i)/points3D.at<float>(3,i), points3D.at<float>(1,i)/points3D.at<float>(3,i),points3D.at<float>(2,i)/points3D.at<float>(3,i)));
			projectionOn3.push_back(cv::Point2f(projPoints3.at<cv::Vec2f>(i,0)[0],projPoints3.at<cv::Vec2f>(i,0)[1]));
		}
//		if ( i > 10)
//			break;
	}

	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
					"Solve PNP points: %d \n", tmp.size());

	cv::Mat rvec;
	//bool val = solvePnP(tmp, projPoints3, cameraMatrix, distCoeffs, rvec, translation, false, flag); // CV_P3P CV_EPNP
	//bool val = false;
	//solvePnPRansac(tmp, projPoints3, cameraMatrix, distCoeffs, rvec, translation);
	solvePnPRansac(tmp, projectionOn3, cameraMatrix, distCoeffs, rvec, translation, true, 20000, 1.0, tmp.size()/2);

	cv::Rodrigues(rvec, rotation);
	rotation.convertTo(rotation, CV_64F);
	translation.convertTo(translation, CV_64F);

	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
				"type rows cols: %d %d %d \n", translation.type(), translation.rows, translation.cols);
	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
					"tx ty tz: %f %f %f \n", translation.at<double>(0,0), translation.at<double>(1,0), translation.at<double>(2,0));


	//
	//bool useExtrinsicGuess=false,
	//,int iterationsCount=100, float reprojectionError=8.0, int minInliersCount=100, OutputArray inliers=noArray(), int flags=ITERATIVE )
}

// Solves equation:
// [-T23 T13] * [s23 s13]' = [T12 * s12]
// Known values:
// T12, T23, T13 -> translations in columns
// s12 -> previous scale
// Unknowns:
// s23, s13 -> scales to find
double estimateScaleTriangle(Eigen::Vector3d trans12, Eigen::Vector3d trans13, Eigen::Vector3d trans23, double scale12)
{
	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
				"estScale 12: %f %f %f\n", trans12(0),trans12(1), trans12(2));
	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
					"estScale 13: %f %f %f\n", trans13(0),trans13(1), trans13(2));
	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
					"estScale 23: %f %f %f\n", trans23(0),trans23(1), trans23(2));
	Eigen::MatrixXd leftSide = Eigen::MatrixXd::Zero(3, 2);
	leftSide.block<3,1>(0,0) = - trans23;
	leftSide.block<3,1>(0,1) = trans13;

	Eigen::VectorXd rightSide = Eigen::VectorXd::Zero(3);
	rightSide.block<3,1>(0,0) = scale12 * trans12;

	Eigen::VectorXd unknowns = Eigen::VectorXd::Zero(2);
//	unknowns = leftSide.jacobiSvd(Eigen::ComputeThinU | Eigen::ComputeThinV).solve(rightSide);
	return unknowns(0);
}

double estimateScaleTriangle(cv::Mat trans12, cv::Mat trans13, cv::Mat trans23, double scale12)
{
	Eigen::Vector3d t12(trans12.at<double>(0,0), trans12.at<double>(1,0), trans12.at<double>(2,0));
	Eigen::Vector3d t13(trans13.at<double>(0,0), trans13.at<double>(1,0), trans13.at<double>(2,0));
	Eigen::Vector3d t23(trans23.at<double>(0,0), trans23.at<double>(1,0), trans23.at<double>(2,0));
	return estimateScaleTriangle(t12, t13, t23, scale12);
}
