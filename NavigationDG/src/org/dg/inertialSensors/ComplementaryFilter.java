/*
 * Author: Jan Wietrzykowski
 */
package org.dg.inertialSensors;
import java.util.concurrent.Semaphore;

import android.hardware.SensorManager;
import android.util.Log;

public class ComplementaryFilter {
	// TAG
	private static final String TAG = "ComplementaryFilter";

	private static final float eps = 1e-9f;
//	private static final float ns2s = 1.0f / 1000000000.0f;
	private float coeff = 0.9992f;
	
	private boolean init = true;
	
	private float[] gyroMat = null;
	private float[] accMagMat =  new float[9];

	private final Semaphore fusedMtx = new Semaphore(1, true);
	private float[] fusedQuat = new float[4];
	
	// information whether the estimation is running
	boolean started;

	//http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	private void compQuatFromGyro(float[] gyroVals,
									float dt,
									float[] quat)
	{
		float[] gyroValsNorm = new float[]{0.0f, 0.0f, 0.0f};
		float norm = (float)Math.sqrt(gyroVals[0] * gyroVals[0] + 
									gyroVals[1] * gyroVals[1] + 
									gyroVals[2] * gyroVals[2]);
		if(norm > eps){
			for(int v = 0; v < 3; v++){
				gyroValsNorm[v] = gyroVals[v] / norm;
			}
		}
		
		float thetaOverTwo = norm * dt / 2.0f;
		float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
		float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);

		quat[0] = cosThetaOverTwo;
		quat[1] = sinThetaOverTwo * gyroValsNorm[0];
		quat[2] = sinThetaOverTwo * gyroValsNorm[1];
		quat[3] = sinThetaOverTwo * gyroValsNorm[2];
	}
	
	public static float[] quat2rotVec(float[] quat)
	{
		float mul = quat[0] > 0 ? 1.0f : -1.0f;
		float[] rotVec = new float[]{mul*quat[1],
									mul*quat[2],
									mul*quat[3]};
		return rotVec;
	}
	
	public static float[] orient2mat(float[] orient)
	{
		float[] mat = new float[]{1.0f, 0.0f, 0.0f,
									0.0f, 1.0f, 0.0f,
									0.0f, 0.0f, 1.0f};
		
		float sinOx = (float)Math.sin(orient[1]);
		float cosOx = (float)Math.cos(orient[1]);
		float sinOy = (float)Math.sin(orient[2]);
		float cosOy = (float)Math.cos(orient[2]);
		float sinOz = (float)Math.sin(orient[0]);
		float cosOz = (float)Math.cos(orient[0]);
		

		float[] matX = new float[]{1.0f, 0.0f, 0.0f,
									0.0f, cosOx, sinOx,
									0.0f, -sinOx, cosOx};
		
		float[] matY = new float[]{cosOy, 0.0f, sinOy,
									0.0f, 1.0f, 0.0f,
									-sinOy, 0.0f, cosOy};
		
		float[] matZ = new float[]{cosOz, sinOz, 0.0f,
									-sinOz, cosOz, 0.0f,
									0.0f, 0.0f, 1.0f};
		
		mat = matMul(mat, matZ);
		mat = matMul(mat, matX);
		mat = matMul(mat, matY);
		
		return mat;
	}
	
	//http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
	public static float[] mat2quat(float[] mat)
	{
		float[] quat = new float[4];
		
		float tr = mat[0*3 + 0] + mat[1*3 + 1] + mat[2*3 + 2];
		if(tr > 0){
			float S = (float)Math.sqrt(tr + 1.0) * 2.0f;
			quat[0] = 0.25f * S;
			quat[1] = (mat[2*3 + 1] - mat[1*3 + 2]) / S;
			quat[2] = (mat[0*3 + 2] - mat[2*3 + 0]) / S;
			quat[3] = (mat[1*3 + 0] - mat[0*3 + 1]) / S;
		}
		else if((mat[0*3 + 0] > mat[1*3 + 1]) && (mat[0*3 + 0] > mat[2*3 + 2])){
			float S = (float)Math.sqrt(1.0f + mat[0*3 + 0] - mat[1*3 + 1] - mat[2*3 + 2]) * 2.0f;
			quat[0] = (mat[2*3 + 1] - mat[1*3 + 2]) / S;
			quat[1] = 0.25f * S;
			quat[2] = (mat[0*3 + 1] + mat[1*3 + 0]) / S;
			quat[3] = (mat[0*3 + 2] + mat[2*3 + 0]) / S;
			
		}
		else if(mat[1*3 + 1] > mat[2*3 + 2]){
			float S = (float)Math.sqrt(1.0f - mat[0*3 + 0] + mat[1*3 + 1] - mat[2*3 + 2]) * 2.0f;
			quat[0] = (mat[0*3 + 2] - mat[2*3 + 0]) / S;
			quat[1] = (mat[0*3 + 1] + mat[1*3 + 0]) / S;
			quat[2] = 0.25f * S;
			quat[3] = (mat[1*3 + 2] + mat[2*3 + 1]) / S;
		}
		else{
			float S = (float)Math.sqrt(1.0f - mat[0*3 + 0] - mat[1*3 + 1] + mat[2*3 + 2]) * 2.0f;
			quat[0] = (mat[1*3 + 0] - mat[0*3 + 1]) / S;
			quat[1] = (mat[0*3 + 2] + mat[2*3 + 0]) / S;
			quat[2] = (mat[1*3 + 2] + mat[2*3 + 1]) / S;
			quat[3] = 0.25f * S;
		}
		
		return quat;
	}
	
	public static float[] matMul(float[] A, float[] B)
	{
		float[] ret = new float[9];
		
		for(int i = 0; i < 3; i++){
			for(int j = 0; j < 3; j++){
				ret[i*3 + j] = 0.0f;
				for(int k = 0; k < 3; k++){
					ret[i*3 + j] += A[i*3 + k] * B[k*3 + j];
				}
			}
		}
		
		return ret;
	}
	
	private float[] compFilter(float[] gyroOrient,
								float[] accMagOrient)
	{
		float[] fusedOrient = new float[3];
		
		for(int ang = 0; ang < 3; ang++){
			if(Math.abs(gyroOrient[ang] - accMagOrient[ang]) < Math.abs(Math.abs(gyroOrient[ang] - accMagOrient[ang]) - 2.0 * Math.PI)){
				fusedOrient[ang] = gyroOrient[ang] * coeff + accMagOrient[ang] * (1 - coeff);
			}
			else{
				fusedOrient[ang] = (gyroOrient[ang] + (gyroOrient[ang] < 0.0f ? 2.0f * (float)Math.PI : 0.0f)) * coeff +
								(accMagOrient[ang] + (accMagOrient[ang] < 0.0f ? 2.0f * (float)Math.PI : 0.0f))* (1 - coeff);
				fusedOrient[ang] = fusedOrient[ang] - (fusedOrient[ang] > (float)Math.PI ? 2.0f * (float)Math.PI : 0.0f);
			}
		}
		
		return fusedOrient;
	}
	
	private void updateFilter()
	{
		float[] gyroOrient = new float[3];
		SensorManager.getOrientation(gyroMat, gyroOrient);
//		Log.d(TAG, String.format("gyro orient = \n {%f, %f, %f}\n",
//								gyroOrient[0],
//								gyroOrient[1],
//								gyroOrient[2]));
		
		float[] accMagOrient = new float[3];
		SensorManager.getOrientation(accMagMat, accMagOrient);
//		Log.d(TAG, String.format("accMag orient = \n {%f, %f, %f}\n",
//								accMagOrient[0],
//								accMagOrient[1],
//								accMagOrient[2]));
		
		float[] fusedOrient = compFilter(gyroOrient,
											accMagOrient);
		
//		Log.d(TAG, String.format("estimated orient = \n {%f, %f, %f}\n",
//								fusedOrient[0],
//								fusedOrient[1],
//								fusedOrient[2]));
		
		
		float[] fusedMat = orient2mat(fusedOrient);
		
		gyroMat = fusedMat.clone();
		
		try {
			fusedMtx.acquire();
			fusedQuat = mat2quat(fusedMat);
			
//			Log.d(TAG, String.format("estimated mat = \n {%f, %f, %f,\n"
//									+ "%f, %f, %f,\n"
//									+ "%f, %f, %f}\n",
//									fusedMat[0], fusedMat[1], fusedMat[2],
//									fusedMat[3], fusedMat[4], fusedMat[5],
//									fusedMat[6], fusedMat[7], fusedMat[8]));
									
			
			fusedMtx.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public ComplementaryFilter(float icoeff) {
		coeff = icoeff;
		// JW: if you need to start the complementary filter -> change to true
		// Then after starting application you need to start the Inertial
		// sensors by "Start inertial sensors"
		start();
	}

	// Start complementary filter estimation
	public void start() {
		init = true;
		started = true;
		Log.d(TAG, String.format("complementary filter started"));
	}

	// Stop complementary filter estimation
	public void stop() {
		started = false;
	}

	// return the state of estimation
	public boolean getState() {
		return started;
	}

	// Return the current estimate as a quaternion: [qw qx qy qz]
	public float[] getEstimate() {
		float[] retFusedQuat = new float[3];
		
		try {
			fusedMtx.acquire();
			retFusedQuat = fusedQuat.clone();
			fusedMtx.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return retFusedQuat;
	}
	
	// / Information from gyroscopes (feel free to change the method's name):
	// float [] gyro = [gyroX, gyroY, gyroZ] in angles/s,
	// float dt = (time from last gyro update in seconds)
	public void gyroscopeUpdate(float[] gyro, float dt) {
//		Log.d(TAG, String.format(
//				"gyroscopeUpdate: gyro=[%f %f %f] and dt=%f!\n", gyro[0],
//				gyro[1], gyro[2], dt));
		
		if((dt > 1e-3f) && (gyroMat != null)){
			float[] deltaQuat = new float[4];
			compQuatFromGyro(gyro, dt, deltaQuat);
			float[] deltaMat = new float[9];
			SensorManager.getRotationMatrixFromVector(deltaMat, quat2rotVec(deltaQuat));
			
//			Log.d(TAG, String.format("delta mat = \n {%f, %f, %f,\n"
//									+ "%f, %f, %f,\n"
//									+ "%f, %f, %f}\n",
//									deltaMat[0], deltaMat[1], deltaMat[2],
//									deltaMat[3], deltaMat[4], deltaMat[5],
//									deltaMat[6], deltaMat[7], deltaMat[8]));
			
			gyroMat = matMul(gyroMat, deltaMat);
			
			updateFilter();
		}
	}

	// / Information from accelerometers/magnetometers (feel free to change the
	// method's name):
	// float [] quaternion = [quaternionW, quaternionX, quaternionY,
	// quaternionZ]
	//
	public void magAccUpdate(float[] quat) {
//		Log.d(TAG, String.format("magAccUpdate: quat=[%f %f %f %f]\n",
//				quat[0], quat[1], quat[2], quat[3]));
		
		SensorManager.getRotationMatrixFromVector(accMagMat, quat2rotVec(quat));
		
//		Log.d(TAG, String.format("accMag mat = \n {%f, %f, %f,\n"
//								+ "%f, %f, %f,\n"
//								+ "%f, %f, %f}\n",
//								accMagMat[0], accMagMat[1], accMagMat[2],
//								accMagMat[3], accMagMat[4], accMagMat[5],
//								accMagMat[6], accMagMat[7], accMagMat[8]));
		
		if(init){
			gyroMat = accMagMat.clone();
			init = false;
		}
		
		updateFilter();
	}

}
