#define _USE_MATH_DEFINES
#include <cmath>

#include <opencv2/core/core.hpp>
#include <opencv2/calib3d/calib3d.hpp>		// for projectPoints
#include <opencv2/imgproc/imgproc.hpp>		// for undistortPoints
using namespace cv;

#include <iostream>
#include <fstream>
#include <iomanip>							// for std::setw
//using namespace std;

#include <windows.h>


#include "visualodometry.h"					// for Adam code
#include "FivePointAlgorithm.h"					// for five-point solver

void Adam_PointGenerator(std::vector<cv::Point2f> &vec_image_points_pre, std::vector<cv::Point2f> &vec_image_points_post, cv::Mat &rotationOriginal, cv::Mat &translationOriginal, double& errorOriginal)
{
	double angle = M_PI / 10.;//15deg
	double axis_x = sqrt(2.) / 2.;
	double axis_y = -sqrt(2.) / 2.;
	double axis_z = 0;

	Mat quaternion(4, 1, CV_64FC1);
	quaternion.at<double>(0) = cos(angle / 2.);
	quaternion.at<double>(1) = axis_x*sin(angle / 2.);
	quaternion.at<double>(2) = axis_y*sin(angle / 2.);
	quaternion.at<double>(3) = axis_z*sin(angle / 2.);

	Mat rotation;
	VisualOdometry::quaternionToRotation(quaternion, rotation);
	Mat translation(3, 1, CV_64FC1);
	translation.at<double>(0) = 0.7;
	translation.at<double>(1) = -0.3;
	translation.at<double>(2) = 0.6;

	Mat camMat1 = Mat::eye(3, 3, CV_64FC1);
	camMat1.at<double>(0, 0) = 600;
	camMat1.at<double>(1, 1) = 600;
	camMat1.at<double>(0, 2) = 250;
	camMat1.at<double>(1, 2) = 250;
	Mat distMat1 = Mat::zeros(1, 8, CV_64FC1);
	//distMat1.at<double>(0) = .1;
	//distMat1.at<double>(1) = .2;
	//distMat1.at<double>(2) = .3;

	int width1 = 500;
	int height1 = 500;

	Mat camMat2 = Mat::eye(3, 3, CV_64FC1);
	camMat2.at<double>(0, 0) = 600;
	camMat2.at<double>(1, 1) = 600;
	camMat2.at<double>(0, 2) = 250;
	camMat2.at<double>(1, 2) = 250;
	Mat distMat2 = Mat::zeros(1, 8, CV_64FC1);

	int width2 = 500;
	int height2 = 500;

	//add points
	int nPoints = 500;
	double outliers = 0.0;
	int index = 0;
	int i = 0;
	int nInliers = (1. - outliers)*nPoints;
	int nOutliers = nPoints - nInliers;

	double error = 1. / 500.;
	vector<Point2f> points1;
	vector<Point2f> points2;

	while (index<nInliers)
	{
		//random point on image2
		Mat p2(1, 1, CV_64FC2);
		p2.at<Vec2d >(0) = Vec2d(rand() % width2, rand() % height2);

		Mat p2u;
		undistortPoints(p2, p2u, camMat2, distMat2);

		//random depth
		Mat p2InSpace(3, 1, CV_64FC1);

		p2InSpace.at<double>(0) = p2u.at<Vec2d>(0)[0];
		p2InSpace.at<double>(1) = p2u.at<Vec2d>(0)[1];
		p2InSpace.at<double>(2) = 1;

		p2InSpace *= ((rand() % 10000) / 1000. + .5);//0.5-10.5

		Mat p1InSpace = rotation*p2InSpace + translation;
		if (p1InSpace.at<double>(2)>0.5)//positive depth >.5)
		{
			Mat p1(2, 1, CV_64FC1);
			projectPoints(p1InSpace.t(), Mat::zeros(1, 3, CV_64FC1), Mat::zeros(3, 1, CV_64FC1), camMat1, distMat1, p1);

			double u = p1.at<Vec2d>(0)[0];
			double v = p1.at<Vec2d>(0)[1];

			if (u >= 0 && u<width1&& v >= 0 && v<height1)//point on image
			{
				Mat p1u;
				undistortPoints(p1, p1u, camMat1, distMat1);
				double u1noise = ((rand() % 20000) / 10000. - 1)*error;
				double v1noise = ((rand() % 20000) / 10000. - 1)*error;
				double u2noise = ((rand() % 20000) / 10000. - 1)*error;
				double v2noise = ((rand() % 20000) / 10000. - 1)*error;


				points1.push_back(Point2f(p1u.at<Vec2d>(0)[0] + u1noise, p1u.at<Vec2d>(0)[1] + v1noise));
				points2.push_back(Point2f(p2u.at<Vec2d>(0)[0] + u2noise, p2u.at<Vec2d>(0)[1] + v2noise));

				index++;
			}
		}
		i++;
	}
	while (index<nPoints)
	{
		Mat p2(1, 1, CV_64FC2);
		Vec2d v1(rand() % width2, rand() % height2);
		p2.at<Vec2d>(0) = v1;

		Mat p2u;
		undistortPoints(p2, p2u, camMat2, distMat2);


		Mat p1(1, 1, CV_64FC2);
		Vec2d v2(rand() % width2, rand() % height2);
		p1.at<Vec2d>(0) = v2;

		Mat p1u;
		undistortPoints(p1, p1u, camMat1, distMat1);


		points1.push_back(p1u.at<Point2d>(0));
		points2.push_back(p2u.at<Point2d>(0));
		index++;
	}

	errorOriginal = error;

	rotationOriginal = rotation;
	translationOriginal = translation;

	vec_image_points_pre = points1;
	vec_image_points_post = points2;
}

// funkcja do wyœwietlania sformatowanych wartoœci zadanej oraz obliczone rotacji i translacji waraz z przedstawieniem ró¿nicy 
void compareOriginalAndCalculated(cv::Mat originalRotation, cv::Mat originalTranslation, cv::Mat calculatedRotation, cv::Mat calculatedTranslation)
{
	std::ofstream o1;
	o1.open("console.txt", std::fstream::app);

	std::cout.setf(std::ios_base::fixed);
	std::cout.precision(4);

	cv::Mat differenceRotation = originalRotation - calculatedRotation;
	cv::Mat differenceTranslation = originalTranslation - calculatedTranslation;
	
	std::cout << std::endl << "Rotation: " << std::endl;
	std::cout << "Original: \t\t\t" << "Calculated raw: \t\t" << "Difference (R_original - R_calculated):" << std::endl;
	o1 << "Original: \t\t\t" << "Calculated raw: \t\t" << "Difference (R_original - R_calculated):" << std::endl;
	// rotation matrix should be 3x3 with double type
	for (int i = 0; i < 3; ++i)
	{
		for (int j = 0; j < 3; ++j)
		{
			std::cout << std::setw(8) << originalRotation.at<double>(i, j) << ", ";
			o1 << std::setw(8) << originalRotation.at<double>(i, j) << ", ";
		}
		std::cout << "\t";
		o1 << "\t";
		for (int j = 0; j < 3; ++j)
		{

			std::cout << std::setw(8) << calculatedRotation.at<double>(i, j) << ", ";
			o1 << std::setw(8) << calculatedRotation.at<double>(i, j) << ", ";
		}
		std::cout << "\t";
		for (int j = 0; j < 3; ++j)
		{

			std::cout << std::setw(8) << differenceRotation.at<double>(i, j) << ", ";
			o1 << std::setw(8) << differenceRotation.at<double>(i, j) << ", ";
		}
		std::cout << std::endl;
		o1 << std::endl;
	}

	std::cout << "Translation: " << std::endl;
	o1 << "Translation: " << std::endl;
	std::cout << "Original: \t\t\t" << "Calculated raw: \t\t" << "Difference (T_original - T_calculated):" << std::endl;
	o1 << "Original: \t\t\t" << "Calculated raw: \t\t" << "Difference (T_original - T_calculated):" << std::endl;
	{
		for (int j = 0; j < 3; ++j)
		{
			std::cout << std::setw(8) << originalTranslation.at<double>(j) << ", ";
			o1 << std::setw(8) << originalTranslation.at<double>(j) << ", ";
		}
		std::cout << "\t";
		o1 << "\t";
		for (int j = 0; j < 3; ++j)
		{

			std::cout << std::setw(8) << calculatedTranslation.at<double>(j) << ", ";
			o1 << std::setw(8) << calculatedTranslation.at<double>(j) << ", ";
		}
		std::cout << "\t";
		o1 << "\t";
		for (int j = 0; j < 3; ++j)
		{

			std::cout << std::setw(8) << differenceTranslation.at<double>(j) << ", ";
			o1 << std::setw(8) << differenceTranslation.at<double>(j) << ", ";
		}
		std::cout << std::endl;
		o1 << std::endl;
	}
	o1.close();
}

// funkcja konwertuj¹ca punkty w vector<Point2f> (z Adama generatora) na Mat
void ConvertVectorOfPoints2Mat(const std::vector<cv::Point2f>& pointsImgAsVectorOfPoints2f, cv::Mat& pointsImgAsMat)
{
	// (vector<Points2f> -> Mat)
	pointsImgAsMat.create(pointsImgAsVectorOfPoints2f.size(), 2, CV_64FC1);

	for (auto i = 0; i < pointsImgAsVectorOfPoints2f.size(); ++i)
	{
		pointsImgAsMat.at<double>(i, 0) = pointsImgAsVectorOfPoints2f[i].x;
		pointsImgAsMat.at<double>(i, 1) = pointsImgAsVectorOfPoints2f[i].y;
	}

}

int main(void)
{
	// data to fill with generator function
	vector<Point2f> points1;
	vector<Point2f> points2;
	Mat rotation;
	Mat translation;
	double error;

	Adam_PointGenerator(points1, points2, rotation, translation, error);

	{
		std::cout << std::endl << std::endl << "visualodometry module from Adam:";

		Mat R, t;
		// zamieniona kolejnoœæ punktów, bo wewn¹trz tej funkcji Adam u¿ywa ich na odwót :P
		VisualOdometry::findRotationAndTranslation(points1, points2, R, t, error);

		compareOriginalAndCalculated(rotation, translation, R, t);
	}
	// nie dzia³a w 2.4.8
	/*
	{
		std::cout << std::endl << std::endl << "findFundamentalMatrix from OpenCV 3.0:";

		Mat fundamentalMatrixInliers;
		Mat fundamentalMatrix = findFundamentalMat(points2, points1, FM_RANSAC, error, 0.99, fundamentalMatrixInliers);

		Mat R, t;
		recoverPose(fundamentalMatrix, points2, points1, R, t, 1.0, cv::Point2d(0, 0), fundamentalMatrixInliers);
		// W przypadku gdy R ma poprawne wartoœci, ale jest transponowane, a t siê nie zgadza mo¿e wystêpowac problem z kolejnoœci¹ wykonywanych operacji. Sprawdziæ czy poni¿sze dzia³ania da poprany wynik.
		// niezbêdne, gdy¿ Adama generator podaje R i t z uk³adu 2 do 1
		//Mat R2 = R.t();
		//Mat t2 = -R2 * t;
		
		compareOriginalAndCalculated(rotation, translation, R, t);
	}
	{
		std::cout << std::endl << std::endl << "findEssentialMatrix from OpenCV 3.0:";

		Mat essentialMatrixInliers;
		Mat essentialMatrix = findEssentialMat(points2, points1, 1.0, cv::Point2d(0, 0), FM_RANSAC, 0.99, error, essentialMatrixInliers);

		Mat R, t;
		recoverPose(essentialMatrix, points2, points1, R, t, 1.0, cv::Point2d(0, 0), essentialMatrixInliers);

		compareOriginalAndCalculated(rotation, translation, R, t);
	}
	*/
	{

		std::cout << std::endl << std::endl << "myFivePointAlgorithm:";

		// conversion to Mat from vector<Point2f>
		Mat Mat_points1, Mat_points2;
		ConvertVectorOfPoints2Mat(points1, Mat_points1);
		ConvertVectorOfPoints2Mat(points2, Mat_points2);

		// variables to return
		Mat R, t;

		std::cout<<"Chosen error: " << error<< std::endl;
		DWORD st = GetTickCount();
		for (int k=0;k<10;k++)
			FP::RotationTranslationFromFivePointAlgorithm(Mat_points2, Mat_points1, error, R, t);
		DWORD end = GetTickCount();
		std::cout << std::endl << "Measured time : " << (end - st)/10 << std::endl;

		compareOriginalAndCalculated(rotation, translation, R, t);
	}

	std::ofstream o1;
	o1.open("points1.txt");
	for (int i=0;i<points1.size();i++)
	{
		o1<<points1[i].x << " " << points1[i].y << std::endl;
	}
	o1.close();

	std::ofstream o2;
	o2.open("points2.txt");
	for (int i=0;i<points2.size();i++)
	{
		o2<<points2[i].x << " " << points2[i].y << std::endl;
	}
	o2.close();




	std::cout << "Press ENTER to continue...";
	//std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');

	return 0;
}