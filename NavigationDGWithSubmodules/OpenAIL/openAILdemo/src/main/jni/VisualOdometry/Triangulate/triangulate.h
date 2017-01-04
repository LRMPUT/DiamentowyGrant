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
