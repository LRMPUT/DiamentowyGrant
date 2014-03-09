#include "orientationEstSystem.h"

class orientationEstKalman : public orientationEstSystem {
public:
  ~orientationEstKalman(){};

  void updateAcc(float *data);
  void updateGyro(float *data);
  void updateMag(float *data);

  void getEstimate(float *data);
};
