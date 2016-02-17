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
