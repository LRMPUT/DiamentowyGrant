
class orientationEstSystem {
public:
  virtual void updateAcc(float *data) = 0;
  virtual void updateGyro(float *data) = 0;
  virtual void updateMag(float *data) = 0;

  virtual void getEstimate(float *data) = 0;
};
