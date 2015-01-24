#include <opencv2/core/core.hpp>
#include <android/log.h>
#include <pthread.h>

class EKF {

	cv::Mat R, Q;
	cv::Mat homogra;
	cv::Mat w;
	cv::Mat I;
	cv::Mat K;
	float dt;
	cv::Mat x_apriori, x_aposteriori; // 7x1 matrices
	cv::Mat P_apriori, P_aposteriori; // 7x7 matrices
	cv::Mat H;
	cv::Mat state;

public:
	EKF(float _Q, float _R, float dt);
	void predict(long addrW, float dt);
	void correct(long addrZ);
	cv::Mat getState(); // TODO!!!

private:
	pthread_mutex_t stateMtx;
	cv::Mat jacobian(long addrW);
	cv::Mat Astate(long addrW);
};
