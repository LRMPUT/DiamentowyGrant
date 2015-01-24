#include "VOsystem.h"

void VO::eigen2matSave(Eigen::Matrix4f transformation, cv::Mat & motionEstimate, int index)
{
	motionEstimate.at<double>(0, index) = transformation(0, 3);
	motionEstimate.at<double>(1, index) = transformation(1, 3);
	motionEstimate.at<double>(2, index) = transformation(2, 3);
	Eigen::Quaternion<float> Q(transformation.block<3, 3>(0, 0));
	motionEstimate.at<double>(3, index) = Q.x();
	motionEstimate.at<double>(4, index) = Q.y();
	motionEstimate.at<double>(5, index) = Q.z();
	motionEstimate.at<double>(6, index) = Q.w();
}

// tx ty tz qx qy qz qw
// 1.3112 0.8507 1.5186 0.8851 0.2362 -0.0898 -0.3909
Eigen::Matrix4f VO::getInitialPosition()
{
	Eigen::Matrix4f initialPosition = Eigen::Matrix4f::Identity();
	Eigen::Quaternion<float> quat(-0.3909, 0.8851, 0.2362, -0.0898);
	initialPosition.block<3,3>(0,0) = quat.toRotationMatrix();
	//initialPosition.block<3,3>(0,0) = Eigen::Matrix3f::Identity();

	initialPosition(0,3) = 1.3112;
	initialPosition(1,3) = 0.8507;
	initialPosition(2,3) = 1.5186;
	initialPosition(3,3) = 1.0;
//	initialPosition(0, 3) = 0.0;
//	initialPosition(1, 3) = 0.0;
//	initialPosition(2, 3) = 0.0;
//	initialPosition(3, 3) = 0.0;
	return initialPosition;
}

Eigen::Matrix4f VO::mat2eigen(cv::Mat rotation, cv::Mat translationVector)
{
	rotation.convertTo(rotation, CV_64F);
	translationVector.convertTo(translationVector, CV_64F);

	Eigen::Matrix4f transformation = Eigen::Matrix4f::Identity();
	for (int k = 0; k < 3; k++) {
		for (int j = 0; j < 3; j++)
			transformation(k, j) = rotation.at<double>(k, j);
		transformation(k, 3) = translationVector.at<double>(k);
	}
	return transformation;
}

void VO::eigen2mat(Eigen::Matrix4f transformation, cv::Mat &rotationMatrix, cv::Mat &translationVector)
{
	rotationMatrix.convertTo(rotationMatrix, CV_64F);
	translationVector.convertTo(translationVector, CV_64F);
	for (int k = 0; k < 3; k++) {
		for (int j = 0; j < 3; j++)
			rotationMatrix.at<double>(k, j) = transformation(k, j);
		translationVector.at<double>(k) = transformation(k, 3);
	}
}

void VO::eigen2cameraPos(Eigen::Matrix4f transformation, cv::Mat &cameraPos)
{
//	for (int i=0;i<4;i++)
//			__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
//						"Transformation: %f %f %f %f\n", transformation(i,0), transformation(i,1), transformation(i,2), transformation(i,3));


	cameraPos.convertTo(cameraPos, CV_64F);
	for (int k = 0; k < 3; k++) {
		for (int j = 0; j < 3; j++)
			cameraPos.at<double>(k, j) = transformation(k, j);
		cameraPos.at<double>(k, 3) = transformation(k, 3);
	}


//	for (int k = 0; k < 3; k++)
//				__android_log_print(ANDROID_LOG_DEBUG, "MScThesis",
//						"CameraPos: %f %f %f %f\n", cameraPos.at<double>(k,0), cameraPos.at<double>(k,1), cameraPos.at<double>(k,2), cameraPos.at<double>(k,3));


}

void VO::invertTranslation(cv::Mat &rotationMatrix, cv::Mat &translationVector)
{
	Eigen::Matrix4f x = VO::mat2eigen(rotationMatrix, translationVector);
	x = x.inverse();
	VO::eigen2mat(x, rotationMatrix, translationVector);
}

float VO::countInlierRate(cv::Mat inliers)
{

	int ile = 0;
	for (int i = 0; i < inliers.rows; i++) {
		if (inliers.at<unsigned char>(i, 0) == 1) {
			ile++;
		}
	}
	return ile * 100.0 / inliers.rows;
}

void VO::matchedPoints2Mat(std::vector<cv::DMatch> matches, std::vector<cv::KeyPoint> v1, std::vector<cv::KeyPoint> v2, cv::Mat & p1, cv::Mat & p2)
{
	std::vector<cv::Point2f> vec1, vec2;
	for (int z = 0; z < matches.size(); z++) {
		vec1.push_back(v1[matches[z].queryIdx].pt);
		vec2.push_back(v2[matches[z].trainIdx].pt);
	}
	cv::Mat m1(vec1), m2(vec2);
	m1.copyTo(p1);
	m2.copyTo(p2);
}


void VO::matchedPoints3Mat(std::vector<cv::DMatch> matches12, std::vector<cv::DMatch> matches23, std::vector<cv::KeyPoint> v1,
			std::vector<cv::KeyPoint> v2, std::vector<cv::KeyPoint> v3, cv::Mat inliers12, cv::Mat inliers23, cv::Mat & p1, cv::Mat & p2, cv::Mat & p3)
{
	std::vector<cv::Point2f> vTmp[3];

	for (int j = 0; j < matches12.size(); j++) {
		for (int k = 0; k < matches23.size(); k++) {

			if (matches12[j].trainIdx == matches23[k].queryIdx) {
				if (inliers12.at<unsigned char>(j, 1) == 1
						&& inliers23.at<unsigned char>(k, 1) == 1) {
					vTmp[0].push_back(v1[matches12[j].queryIdx].pt);
					vTmp[1].push_back(v2[matches23[k].queryIdx].pt);
					vTmp[2].push_back(v3[matches23[k].trainIdx].pt);
				}
			}
		}
	}
	cv::Mat projPoints1(vTmp[0]), projPoints2(vTmp[1]), projPoints3(vTmp[2]);
	projPoints1.copyTo(p1);
	projPoints2.copyTo(p2);
	projPoints3.copyTo(p3);
}

void VO::matchedPoints3Mat(std::vector<cv::DMatch> matches12, std::vector<cv::DMatch> matches23, std::vector<cv::DMatch> matches13,
		std::vector<cv::KeyPoint> v1, std::vector<cv::KeyPoint> v2, std::vector<cv::KeyPoint> v3,
		cv::Mat inliers12, cv::Mat inliers23, cv::Mat inliers13, cv::Mat & p1, cv::Mat & p2, cv::Mat & p3)
{
//	std::vector<cv::Point2f> vTmp[3];
//
//	for (int j = 0; j < matches12.size(); j++) {
//		for (int k = 0; k < matches23.size(); k++) {
//			for (int i = 0; i < matches13.size(); i++) {
//				if (matches12[j].trainIdx == matches23[k].queryIdx && matches12[j].queryIdx == matches13[i].queryIdx && matches23[k].trainIdx == matches13[i].trainIdx) {
//					if (inliers12.at<unsigned char>(j, 1) == 1
//							&& inliers23.at<unsigned char>(k, 1) == 1
//							&& inliers13.at<unsigned char>(i, 1) == 1) {
//						vTmp[0].push_back(v1[matches12[j].queryIdx].pt);
//						vTmp[1].push_back(v2[matches23[k].queryIdx].pt);
//						vTmp[2].push_back(v3[matches23[k].trainIdx].pt);
//					}
//				}
//			}
//		}
//	}
//	cv::Mat projPoints1(vTmp[0]), projPoints2(vTmp[1]), projPoints3(vTmp[2]);
//	projPoints1.copyTo(p1);
//	projPoints2.copyTo(p2);
//	projPoints3.copyTo(p3);


	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis", "1\n");

	std::vector<cv::Point2f> vTmp[3];
	cv::Mat toCpy[3];
	toCpy[0] = cv::Mat::zeros(v1.size(), 1, CV_32S);
	toCpy[1] = cv::Mat::zeros(v2.size(), 1, CV_32S);
	toCpy[2] = cv::Mat::zeros(v3.size(), 1, CV_32S);

	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis", "2\n");

	for (int j = 0; j < matches12.size(); j++) {
		if (inliers12.at<unsigned char>(j, 1) == 1)
		{
			toCpy[0].at<int>( matches12[j].queryIdx ) = matches12[j].trainIdx + 1;
		}
	}

	for (int j = 0; j < matches23.size(); j++) {
		if (inliers23.at<unsigned char>(j, 1) == 1) {
			toCpy[1].at<int>(matches23[j].queryIdx) = matches23[j].trainIdx + 1;
		}
	}

	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis", "3\n");

	for (int j = 0; j < matches13.size(); j++) {
			if (inliers13.at<unsigned char>(j, 1) == 1) {
				int read = toCpy[0].at<int>(matches13[j].queryIdx);
				if ( read > 0)
				{
					int read2 = toCpy[1].at<int>(read - 1);
					if ( read2 - 1 == matches13[j].trainIdx)
					{
						vTmp[0].push_back(v1[matches13[j].queryIdx].pt);
						vTmp[1].push_back(v2[read-1].pt);
						vTmp[2].push_back(v3[read2-1].pt);
					}
				}
			}
		}


	__android_log_print(ANDROID_LOG_DEBUG, "MScThesis", "4\n");

	cv::Mat projPoints1(vTmp[0]), projPoints2(vTmp[1]), projPoints3(vTmp[2]);
	projPoints1.copyTo(p1);
	projPoints2.copyTo(p2);
	projPoints3.copyTo(p3);

}

double VO::norm(cv::Mat translationVector)
{
	translationVector.convertTo(translationVector, CV_64F);
	Eigen::Vector3d x(translationVector.at<double>(0), translationVector.at<double>(1), translationVector.at<double>(2));
	return x.norm();
}

double VO::norm(Eigen::Matrix4f transformation)
{
	Eigen::Vector3f x(transformation(0,3), transformation(1,3), transformation(2,3));
	return x.norm();
}
