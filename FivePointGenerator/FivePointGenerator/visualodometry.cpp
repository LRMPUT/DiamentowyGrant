#include "visualodometry.h"
#include <opencv2/calib3d/calib3d.hpp>

#include <iostream>
using namespace std;


VisualOdometry::VisualOdometry()
{
}

void VisualOdometry::rotationToQuaternion(Mat &rotation, Mat &quaternion)
{
	double R00 = rotation.at<double>(0, 0);
	double R10 = rotation.at<double>(1, 0);
	double R20 = rotation.at<double>(2, 0);

	double R01 = rotation.at<double>(0, 1);
	double R11 = rotation.at<double>(1, 1);
	double R21 = rotation.at<double>(2, 1);

	double R02 = rotation.at<double>(0, 2);
	double R12 = rotation.at<double>(1, 2);
	double R22 = rotation.at<double>(2, 2);

	double r, s, t, rt;

	double trace = R00 + R11 + R22;

	quaternion.create(4, 1, CV_64FC1);
	if (trace>0)
	{
		t = R00 + R11 + R22 + 1;
		r = sqrt(t);
		rt = t*r;
		s = 0.5 / r;
		quaternion.at<double>(0) = 0.5*r;
		quaternion.at<double>(1) = (R21 - R12)*s;
		quaternion.at<double>(2) = (R02 - R20)*s;
		quaternion.at<double>(3) = (R10 - R01)*s;
	}
	else
	{
		double maxTrace = max(R00, max(R11, R22));
		if (R00 == maxTrace)
		{
			t = 1 + R00 - R11 - R22;
			r = sqrt(t);
			rt = t*r;
			s = 0.5 / r;
			quaternion.at<double>(0) = (R21 - R12)*s;
			quaternion.at<double>(1) = 0.5*r;
			quaternion.at<double>(2) = (R01 + R10)*s;
			quaternion.at<double>(3) = (R02 + R20)*s;
		}
		else if (R11 == maxTrace)
		{
			t = 1 + R11 - R00 - R22;
			r = sqrt(t);
			rt = t*r;
			s = 0.5 / r;
			quaternion.at<double>(0) = (R02 - R20)*s;
			quaternion.at<double>(1) = (R01 + R10)*s;
			quaternion.at<double>(2) = 0.5*r;
			quaternion.at<double>(3) = (R12 + R21)*s;
		}
		else
		{
			t = 1 + R22 - R11 - R00;
			r = sqrt(t);
			rt = t*r;
			s = 0.5 / r;
			quaternion.at<double>(0) = (R10 - R01)*s;
			quaternion.at<double>(1) = (R02 + R20)*s;
			quaternion.at<double>(2) = (R21 + R12)*s;
			quaternion.at<double>(3) = 0.5*r;
		}
	}
}

void VisualOdometry::quaternionToRotation(Mat &quaternion, Mat &rotation)
{
	double qa = quaternion.at<double>(0);
	double qb = quaternion.at<double>(1);
	double qc = quaternion.at<double>(2);
	double qd = quaternion.at<double>(3);

	rotation.create(3, 3, CV_64FC1);

	rotation.at<double>(0, 0) = qa*qa + qb*qb - qc*qc - qd*qd;
	rotation.at<double>(0, 1) = 2 * (qb*qc - qa*qd);
	rotation.at<double>(0, 2) = 2 * (qb*qd + qa*qc);

	rotation.at<double>(1, 0) = 2 * (qb*qc + qa*qd);
	rotation.at<double>(1, 1) = qa*qa - qb*qb + qc*qc - qd*qd;
	rotation.at<double>(1, 2) = 2 * (qc*qd - qa*qb);

	rotation.at<double>(2, 0) = 2 * (qb*qd - qa*qc);
	rotation.at<double>(2, 1) = 2 * (qc*qd + qa*qb);
	rotation.at<double>(2, 2) = qa*qa - qb*qb - qc*qc + qd*qd;
}

int VisualOdometry::findRotationAndTranslation(std::vector<Point2f> &points1, std::vector<Point2f> &points2, Mat &R, Mat &t, double error)
{
	Mat status;
	Mat essential = findFundamentalMat(points2, points1, status, FM_RANSAC/*FM_8POINT*/, error, .999);

	SVD decomp(essential);
	decomp.u *= determinant(decomp.u);
	decomp.vt *= determinant(decomp.vt);
	Mat W = Mat::zeros(3, 3, CV_64FC1);
	W.at<double>(0, 1) = -1;
	W.at<double>(1, 0) = 1;
	W.at<double>(2, 2) = 1;

	Mat sigmaM = Mat::zeros(3, 3, CV_64FC1);
	sigmaM.at<double>(0, 0) = decomp.w.at<double>(0);
	sigmaM.at<double>(1, 1) = decomp.w.at<double>(1);
	sigmaM.at<double>(2, 2) = decomp.w.at<double>(2);


	Mat R1 = decomp.u*W.t()*decomp.vt;
	Mat R2 = decomp.u*W*decomp.vt;


	Mat t1 = decomp.u.col(2);
	Mat t2 = -t1;

	Mat T = Mat::eye(4, 4, CV_64FC1);

	Mat T11 = Mat::eye(4, 4, CV_64FC1);
	Mat T12 = Mat::eye(4, 4, CV_64FC1);
	Mat T21 = Mat::eye(4, 4, CV_64FC1);
	Mat T22 = Mat::eye(4, 4, CV_64FC1);

	R1.copyTo(Mat(T11, Rect(0, 0, 3, 3)));
	t1.copyTo(Mat(T11, Rect(3, 0, 1, 3)));

	R1.copyTo(Mat(T12, Rect(0, 0, 3, 3)));
	t2.copyTo(Mat(T12, Rect(3, 0, 1, 3)));

	R2.copyTo(Mat(T21, Rect(0, 0, 3, 3)));
	t1.copyTo(Mat(T21, Rect(3, 0, 1, 3)));

	R2.copyTo(Mat(T22, Rect(0, 0, 3, 3)));
	t2.copyTo(Mat(T22, Rect(3, 0, 1, 3)));



	Mat triangulated11;
	Mat triangulated12;
	Mat triangulated21;
	Mat triangulated22;

	Mat T11f, T12f, T21f, T22f;
	T11.convertTo(T11f, CV_32F);
	T12.convertTo(T12f, CV_32F);
	T21.convertTo(T21f, CV_32F);
	T22.convertTo(T22f, CV_32F);

	std::vector<Point2f> points1In, points2In;

	for (int i = 0; i<points1.size(); i++)
	{
		if (status.at<uchar>(i) != 0)
		{
			points1In.push_back(points1[i]);
			points2In.push_back(points2[i]);
		}
	}

	cout << "inliers " << sum(status)[0] << endl;

	triangulatePoints(T.rowRange(0, 3), T11.rowRange(0, 3), points2In, points1In, triangulated11);
	triangulatePoints(T.rowRange(0, 3), T12.rowRange(0, 3), points2In, points1In, triangulated12);
	triangulatePoints(T.rowRange(0, 3), T21.rowRange(0, 3), points2In, points1In, triangulated21);
	triangulatePoints(T.rowRange(0, 3), T22.rowRange(0, 3), points2In, points1In, triangulated22);

	Mat triangulated11S = T11f*triangulated11;
	Mat triangulated12S = T12f*triangulated12;
	Mat triangulated21S = T21f*triangulated21;
	Mat triangulated22S = T22f*triangulated22;

	int votes11 = 0;
	int votes12 = 0;
	int votes21 = 0;
	int votes22 = 0;

	int nP = triangulated11.cols;
	for (int i = 0; i<nP; i++)
	{
		if (triangulated11.at<float>(2, i) / triangulated11.at<float>(3, i)>0 && triangulated11S.at<float>(2, i) / triangulated11S.at<float>(3, i)>0)
			votes11++;
		if (triangulated12.at<float>(2, i) / triangulated12.at<float>(3, i)>0 && triangulated12S.at<float>(2, i) / triangulated12S.at<float>(3, i)>0)
			votes12++;
		if (triangulated21.at<float>(2, i) / triangulated21.at<float>(3, i)>0 && triangulated21S.at<float>(2, i) / triangulated21S.at<float>(3, i)>0)
			votes21++;
		if (triangulated22.at<float>(2, i) / triangulated22.at<float>(3, i)>0 && triangulated22S.at<float>(2, i) / triangulated22S.at<float>(3, i)>0)
			votes22++;
	}


	int maxVotes = max(max(votes11, votes12), max(votes21, votes22));



	if (votes11 == maxVotes)
	{
		R1.copyTo(R);
		t1.copyTo(t);
	}
	if (votes12 == maxVotes)
	{
		R1.copyTo(R);
		t2.copyTo(t);
	}
	if (votes21 == maxVotes)
	{
		R2.copyTo(R);
		t1.copyTo(t);
	}
	if (votes22 == maxVotes)
	{
		R2.copyTo(R);
		t2.copyTo(t);
	}
	cout << votes11 << " " << votes12 << " " << votes21 << " " << votes22 << endl;

	return maxVotes;
}
