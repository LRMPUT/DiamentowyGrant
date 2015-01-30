#include <jni.h>
#include <opencv2/core/core.hpp>


#include "../Eigen/Eigen"


#include <vector>
#include <android/log.h>

class VO
{
public:
	static void eigen2matSave(Eigen::Matrix4f transformation, cv::Mat& motionEstimate, int index);
	static Eigen::Matrix4f getInitialPosition();

	static Eigen::Matrix4f mat2eigen(cv::Mat rotation, cv::Mat translationVector);
	static void eigen2mat(Eigen::Matrix4f transformation, cv::Mat &rotationMatrix, cv::Mat &translationVector);
	static void eigen2cameraPos(Eigen::Matrix4f transformation, cv::Mat &cameraPos);

	static void invertTranslation(cv::Mat &rotationMatrix, cv::Mat &translationVector);

	static float countInlierRate(cv::Mat inliers);

	static void matchedPoints2Mat(std::vector<cv::DMatch> matches, std::vector<cv::KeyPoint> v1, std::vector<cv::KeyPoint> v2, cv::Mat & p1, cv::Mat & p2);
	static void matchedPoints3Mat(std::vector<cv::DMatch> matches12, std::vector<cv::DMatch> matches23, std::vector<cv::KeyPoint> v1,
			std::vector<cv::KeyPoint> v2, std::vector<cv::KeyPoint> v3, cv::Mat inliers12, cv::Mat inliers23, cv::Mat & p1, cv::Mat & p2, cv::Mat & p3);
	static void matchedPoints3Mat(std::vector<cv::DMatch> matches12, std::vector<cv::DMatch> matches23, std::vector<cv::DMatch> matches13, std::vector<cv::KeyPoint> v1,
				std::vector<cv::KeyPoint> v2, std::vector<cv::KeyPoint> v3, cv::Mat inliers12, cv::Mat inliers23, cv::Mat inliers13, cv::Mat & p1, cv::Mat & p2, cv::Mat & p3);

	static double norm(cv::Mat translationVector);
	static double norm(Eigen::Matrix4f transformation);
};
