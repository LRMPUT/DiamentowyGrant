#include "ScaleEKF.h"

EKF::EKF(float _Q, float _R, float _dt) {
	this->Q = cv::Mat::eye(4, 4, CV_32F) * _Q;
	this->R = cv::Mat::eye(4, 4, CV_32F) * _R;

	this->I = cv::Mat::eye(4, 4, CV_32F);
	this->K = cv::Mat::zeros(4, 4, CV_32F);
	this->dt = _dt;
	this->x_apriori = cv::Mat::zeros(4, 1, CV_32F);
	this->x_aposteriori = cv::Mat::zeros(4, 1, CV_32F);
	this->P_apriori = cv::Mat::zeros(4, 4, CV_32F);
	this->P_aposteriori = cv::Mat::zeros(4, 4, CV_32F);
}

cv::Mat EKF::getJacobian() {
	cv::Mat F = cv::Mat::zeros(4, 4, CV_32F);

	// 1st row
	// [1, dt/lambda, dt*dt/lambda, -dt/(lambda*lambda)-dt*dt/(2*lambda*lambda)]
	F.at<float>(0, 0) = 1.0;
	F.at<float>(0, 1) = this->dt / this->x_apriori.at<float>(3);
	F.at<float>(0, 2) = this->dt * this->dt / this->x_apriori.at<float>(3);
	F.at<float>(0, 3) = -this->dt
			/ (this->x_apriori.at<float>(3) * this->x_apriori.at<float>(3))
			- this->dt * this->dt
					/ (2.0 * this->x_apriori.at<float>(3)
							* this->x_apriori.at<float>(3));

	// 2nd row
	F.at<float>(1, 1) = 1.0;
	F.at<float>(1, 2) = this->dt;

	// 3rd row
	F.at<float>(2, 2) = 1.0;

	// 4th row
	F.at<float>(3, 3) = 1;

	return F;
}

cv::Mat EKF::calcNewState() {
	cv::Mat A = cv::Mat::zeros(4, 4, CV_32F);
	// A 1st row
	A.at<float>(0, 0) = 1.0;
	A.at<float>(0, 1) = this->dt / this->x_apriori.at<float>(3);
	A.at<float>(0, 2) = this->dt * this->dt / this->x_apriori.at<float>(3);

	// A 2nd row
	A.at<float>(1, 1) = 1;
	A.at<float>(1, 2) = this->dt;

	// A 3rd row
	A.at<float>(2, 2) = 1;

	// A 4th row
	A.at<float>(3, 3) = 1;

	return A * this->x_aposteriori;
}

cv::Mat EKF::getEstimate() {
	return this->x_aposteriori;
}


void EKF::predict(float _dt) {
	this->dt = _dt;
	this->x_apriori = this->calcNewState();
	cv::Mat F = this->getJacobian();
	this->P_apriori = F * this->P_aposteriori * F.t() + this->Q;
}

void EKF::correctAcc(long addrZ)
{
	cv::Mat Hacc = cv::Mat::zeros(1, 4, CV_32F);
	Hacc.at<float>(2, 2) = 1.0;
	correct(Hacc, addrZ);
}

void EKF::correctVision(long addrZ)
{
	cv::Mat Hvision = cv::Mat::zeros(1, 4, CV_32F);
	Hvision.at<float>(0, 0) = 1.0;
	correct(Hvision, addrZ);
}

void EKF::correctQR(long addrZ)
{
	cv::Mat Hqr = cv::Mat::zeros(1, 4, CV_32F);
	Hqr.at<float>(3, 3) = 1.0;
	correct(Hqr, addrZ);
}

void EKF::correct(cv::Mat H, long addrZ) {
	cv::Mat &z = *(cv::Mat *) addrZ;
	this->K = (this->P_apriori * H.t())
			* (H * this->P_apriori * H.t() + this->R).inv();
	this->x_aposteriori = this->x_apriori
			+ this->K * (z - H * this->x_apriori);
	this->P_aposteriori = (this->I - this->K * H) * this->P_apriori;
}
