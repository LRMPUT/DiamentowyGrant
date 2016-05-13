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

#include "../Eigen/Eigen"
#include <deque>

class EKF {

private:
	// Correct/Predict uncertainty
	Eigen::Matrix<double, 4, 4> R;
	Eigen::Matrix<double, 7, 7> Q, Qmin, Qmax, Qdiff;

	// State estimates (apriori and posteriori)
	Eigen::Matrix<double, 7, 1> x_apriori, x_aposteriori;

	// State estimates uncertainties (apriori and posteriori)
	Eigen::Matrix<double, 7, 7> P_apriori, P_aposteriori;
	Eigen::Matrix<double, 4, 7> H;

	// Additional values to detect estimation start and distinguish between predict/correct order
	bool firstMeasurement;
	bool correctTime;

	// Measurement window used in AEKF
	std::deque<double> measurementWindow;
	static const int measurementWindowSize = 10;

public:
	// Constructor:
	// - Qq, Qw, Rr meaning is the same as in the article published in IEEE Sensors Journal:
	// J. Goslinski, M. Nowicki, P. Skrzypczynski, "Performance Comparison of EKF-based Algorithms for Orientation Estimation Android Platform"
	EKF(float Qqmin, float Qwmin, float Qqmax, float Qwmax, float Rr);

	// Prediction step
	// - takes gyroscope measurements
	// - returns currentEstimate
	void predict(float *inputArray, float dt, float *currentEstimate);

	// Correction step
	// - takes accelerometer and magnetometer coordinate system
	// - returns currentEstimate
	void correct(float *measurement, float *currentEstimate);

private:
	// Additional methods to compute the jacobian
	Eigen::Matrix<double, 7, 7> jacobian(float* w, float dt);

	// Predict based on last estimate and gyroscope measurement
	Eigen::Matrix<double, 7, 1> statePrediction(float* w, float dt);
};
