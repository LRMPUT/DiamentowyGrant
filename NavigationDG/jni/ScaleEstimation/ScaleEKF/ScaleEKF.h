#include <opencv2/core/core.hpp>
#include <android/log.h>

class EKF {

	cv::Mat R, Q;
	cv::Mat homogra;
	cv::Mat w;
	cv::Mat I;
	cv::Mat K;
	float dt;
	cv::Mat x_apriori, x_aposteriori; // 4x1 matrices
	cv::Mat P_apriori, P_aposteriori; // 4x4 matrices

public:
	EKF(float _Q, float _R, float dt);
	void predict(float dt);

	cv::Mat getEstimate();

	void correctAcc(long addrZ);
	void correctVision(long addrZ);
	void correctQR(long addrZ);


private:
	void correct(cv::Mat H, long addrZ);
	cv::Mat getJacobian();
	cv::Mat calcNewState();
};
