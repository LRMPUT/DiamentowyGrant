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

#include "EKF.h"
#include <android/log.h>

EKF::EKF(float Qqmin, float Qwmin, float Qqmax, float Qwmax, float Rr) {

	//
	// Updating covariances based on init values
	//

	// Minimal
	this->Qmin = Eigen::Matrix<double, 7, 7>::Identity();
	this->Qmin.block<4,4>(0,0) = Eigen::Matrix<double, 4, 4>::Identity() * Qqmin;
	this->Qmin.block<3,3>(4,4) = Eigen::Matrix<double, 3, 3>::Identity() * Qwmin;

	// Maximal
	this->Qmax = Eigen::Matrix<double, 7, 7>::Identity();
	this->Qmax.block<4,4>(0,0) = Eigen::Matrix<double, 4, 4>::Identity() * Qqmax;
	this->Qmax.block<3,3>(4,4) = Eigen::Matrix<double, 3, 3>::Identity() * Qwmax;

	// Compute diff
	this->Qdiff = this->Qmax - this->Qmin;

	// Start with minimal
	this->Q = this->Qmax;

	// Covariance of predict
	this->R = Eigen::Matrix<double, 4, 4>::Identity() * Rr;

	// Setting initial values
	this->x_apriori.setZero();
	this->x_aposteriori.setZero();
	this->P_apriori = this->Q * 10000000000000.0;
	this->P_aposteriori = this->Q * 10000000000000.0;

	this->H.setZero();
	for (int i=0;i<4;i++)
		this->H(i, i) = 1.0;

	firstMeasurement = true;
	correctTime = true;
}

Eigen::Matrix<double, 7, 7> EKF::jacobian(float *wArray, float dt) {
	Eigen::Matrix<double, 7, 7> F = Eigen::Matrix<double, 7, 7>::Identity();
	Eigen::Matrix<double, 3, 1> w;
	w << wArray[0], wArray[1], wArray[2];

	// 1st row
	F(0, 0) = 1.0;
	F(0, 1) = -0.5 * (w(0) - this->x_apriori(4)) * dt;
	F(0, 2) = -0.5 * (w(1) - this->x_apriori(5)) * dt;
	F(0, 3) = -0.5 * (w(2) - this->x_apriori(6)) * dt;
	F(0, 4) = 0.5 * dt * this->x_apriori(1);
	F(0, 5) = 0.5 * dt * this->x_apriori(2);
	F(0, 6) = 0.5 * dt * this->x_apriori(3);

	// 2nd row
	F(1, 0) = 0.5 * (w(0) - this->x_apriori(4)) * dt;
	F(1, 1) = 1;
	F(1, 2) = 0.5 * (w(2) - this->x_apriori(6)) * dt;
	F(1, 3) = -0.5 * (w(1) - this->x_apriori(5)) * dt;
	F(1, 4) = -0.5 * dt * this->x_apriori(0);
	F(1, 5) = 0.5 * dt * this->x_apriori(3);
	F(1, 6) = -0.5 * dt * this->x_apriori(2);

	// 3rd row
	F(2, 0) = 0.5 * (w(1) - this->x_apriori(5)) * dt;
	F(2, 1) = -0.5 * (w(2) - this->x_apriori(6)) * dt;
	F(2, 2) = 1;
	F(2, 3) = 0.5 * (w(0) - this->x_apriori(4)) * dt;
	F(2, 4) = -0.5 * dt * this->x_apriori(3);
	F(2, 5) = -0.5 * dt * this->x_apriori(0);
	F(2, 6) = 0.5 * dt * this->x_apriori(1);

	// 4th row
	F(3, 0) = 0.5 * (w(2) - this->x_apriori(6)) * dt;
	F(3, 1) = 0.5 * (w(1) - this->x_apriori(5)) * dt;
	F(3, 2) = -0.5 * (w(0) - this->x_apriori(4)) * dt;
	F(3, 3) = 1;
	F(3, 4) = 0.5 * dt * this->x_apriori(2);
	F(3, 5) = -0.5 * dt * this->x_apriori(1);
	F(3, 6) = -0.5 * dt * this->x_apriori(0);

	// 5th row
	F(4, 4) = 1.0;

	// 6th row
	F(5, 5) = 1.0;

	// 7th row
	F(6, 6) = 1.0;

	return F;
}

Eigen::Matrix<double, 7, 1> EKF::statePrediction(float* wArray, float dt) {
	Eigen::Matrix<double, 7, 1> F = Eigen::Matrix<double, 7, 1>::Identity();
	Eigen::Matrix<double, 3, 1> w;
	w << wArray[0], wArray[1], wArray[2];

	F(4) = this->x_aposteriori(4);
	F(5) = this->x_aposteriori(5);
	F(6) = this->x_aposteriori(6);

	Eigen::Matrix<double, 4, 4> A;
	A.setZero();

	// A 1st row
	A(0, 0) = 1.0;
	A(0, 1) = -0.5 * (w(0) - F(4)) * dt;
	A(0, 2) = -0.5 * (w(1) - F(5)) * dt;
	A(0, 3) = -0.5 * (w(2) - F(6)) * dt;

	// A 2nd row
	A(1, 0) = 0.5 * (w(0) - F(4)) * dt;
	A(1, 1) = 1;
	A(1, 2) = 0.5 * (w(2) - F(6)) * dt;
	A(1, 3) = -0.5 * (w(1) - F(5)) * dt;

	// A 3rd row
	A(2, 0) = 0.5 * (w(1) - F(5)) * dt;
	A(2, 1) = -0.5 * (w(2) - F(6)) * dt;
	A(2, 2) = 1;
	A(2, 3) = 0.5 * (w(0) - F(4)) * dt;

	// A 4th row
	A(3, 0) = 0.5 * (w(2) - F(6)) * dt;
	A(3, 1) = 0.5 * (w(1) - F(5)) * dt;
	A(3, 2) = -0.5 * (w(0) - F(4)) * dt;
	A(3, 3) = 1;

	// Only (1:4)
	Eigen::Matrix<double, 4, 1> x = A * (this->x_aposteriori).block<4, 1>(0, 0);

	for (int i=0;i<4;i++)
		F(i) = x(i);
	return F;
}

void EKF::predict(float* inputArray, float _dt, float *currentEstimate) {

//	__android_log_print(ANDROID_LOG_VERBOSE, "AEKF",
//					"Gyro predict: %.3f %.3f %.3f | %.3f", inputArray[0], inputArray[1], inputArray[2], _dt);

	// We should do predict or correct?
//	if (!correctTime)
//	{
		this->x_apriori = this->statePrediction(inputArray, _dt);
		Eigen::Matrix<double, 7, 7> F = this->jacobian(inputArray, _dt);
		this->P_apriori = F * this->P_aposteriori * F.transpose() + this->Q;
		correctTime = true;
//	}

	// Normalize
	double norm = this->x_apriori.block<4, 1>(0, 0).norm();
	this->x_apriori.block<4, 1>(0, 0) = this->x_apriori.block<4, 1>(0, 0) / norm;

	// Update current estimate
	for (int i=0;i<4;i++)
		currentEstimate[i] = this->x_apriori(i);

//	__android_log_print(ANDROID_LOG_VERBOSE, "AEKF",
//			"Estimate predict: %.3f %.3f %.3f %.3f", currentEstimate[0],
//			currentEstimate[1], currentEstimate[2], currentEstimate[3]);
}

void EKF::correct(float* measurement, float* currentEstimate) {

//	__android_log_print(ANDROID_LOG_VERBOSE, "AEKF",
//				"Measurement correct: %.3f %.3f %.3f %.3f", measurement[0], measurement[1], measurement[2], measurement[3]);

	// First measurement -> we start estimation from acc/mag position
	if(firstMeasurement)
	{
		__android_log_print(ANDROID_LOG_VERBOSE, "AEKF", "Lets see if it works !");

		firstMeasurement = false;
		correctTime = false;
		for (int i=0;i<4;i++)
			this->x_apriori(i) =this->x_aposteriori(i) = measurement[i];

//		this->x_apriori(0) = this->x_aposteriori(0) = 0.7256;
//		this->x_apriori(1) = this->x_aposteriori(1) = 0.1506;
//		this->x_apriori(2) = this->x_aposteriori(2) = -0.1165;
//		this->x_apriori(3) = this->x_aposteriori(3) = -0.6612;
		this->P_aposteriori = this->P_aposteriori * 0.0;
		this->P_apriori = this->P_apriori * 0.0;
	}
	// We should do predict or correct?
	else //if (correctTime)
	{
		correctTime = false;

		// Converting measurements
		Eigen::Matrix<double, 4, 1> z;
		z << measurement[0], measurement[1], measurement[2], measurement[3];

		// Some additional variables
		Eigen::Matrix<double, 7, 7> I = Eigen::Matrix<double, 7, 7>::Identity();
		Eigen::Matrix<double, 7, 4> K = Eigen::Matrix<double, 7, 4>::Zero();


//		for (int i = 0; i<7;i++)
//			__android_log_print(ANDROID_LOG_VERBOSE, "AEKF",
//					"P_ap: %.3f %.3f %.3f %.3f %.3f %.3f %.3f", this->P_apriori(i, 0),
//					this->P_apriori(i, 1), this->P_apriori(i, 2),
//					this->P_apriori(i, 3), this->P_apriori(i, 4),
//					this->P_apriori(i, 5), this->P_apriori(i, 6));

		// EKF equations
		K =
				(this->P_apriori * this->H.transpose())
						* (this->H * this->P_apriori * this->H.transpose()
								+ this->R).inverse();

//		for (int i = 0; i<7;i++)
//			__android_log_print(ANDROID_LOG_VERBOSE, "AEKF",
//						"K: %.3f %.3f %.3f %.3f", K(i,0), K(i,1), K(i,2), K(i,3));

		this->x_aposteriori = this->x_apriori
			+ K * (z - this->H * this->x_apriori);
		this->P_aposteriori = (I - K * this->H) * this->P_apriori;

		//
		// AEKF
		//
		measurementWindow.push_back(measurement[0]);
		while (measurementWindow.size() > measurementWindowSize)
			measurementWindow.pop_front();

		if ( measurementWindow.size() == measurementWindowSize)
		{
			// Mean of elements
			float sum = 0.0f;
			for (int i=0;i<measurementWindowSize;i++)
				sum += measurementWindow[i];
			sum /= measurementWindowSize;

			// Variance
			float var = 0.0f;
			for (int i=0;i<measurementWindowSize;i++)
				var += pow(measurementWindow[i] - sum,2);
			var /= measurementWindowSize;

			// AEKF covariance steering
			if (var < 0.000005)
				this->Q += (this->Qdiff * 0.000005);
			else
				this->Q -= (this->Qdiff * 0.00001);

			// Check the borders of the covariance
			if ( this->Q(0,0) < this->Qmin(0,0) )
				this->Q = this->Qmin;
			else if ( this->Q(0,0) > this->Qmax(0,0) )
				this->Q = this->Qmax;

			//__android_log_print(ANDROID_LOG_VERBOSE, "AEKF", "AEKF val: %f | var: %f", this->Q(0,0)*10000000, var*1000000);
		}
	}

	// Check quat norm
	double norm = this->x_aposteriori.block<4, 1>(0, 0).norm();
	this->x_aposteriori.block<4, 1>(0, 0) = this->x_aposteriori.block<4, 1>(0, 0) / norm;



	// Update current estimate
	for (int i=0;i<4;i++)
		currentEstimate[i] = this->x_aposteriori(i);

//	__android_log_print(ANDROID_LOG_VERBOSE, "AEKF",
//			"Estimate correct: %.3f %.3f %.3f %.3f", currentEstimate[0],
//			currentEstimate[1], currentEstimate[2], currentEstimate[3]);

//	float test = sqrt ( currentEstimate[1]*currentEstimate[1] + currentEstimate[2]*currentEstimate[2] + currentEstimate[3]*currentEstimate[3] );
//	__android_log_print(ANDROID_LOG_VERBOSE, "AEKF", "Quaternion length: %.3f", test );


}
