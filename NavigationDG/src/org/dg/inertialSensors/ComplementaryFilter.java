/*
 * Author: Jan Wietrzykowski
 */
package org.dg.inertialSensors;
import android.util.Log;

public class ComplementaryFilter {
	// TAG
	private static final String TAG = "ComplementaryFilter";

	// information whether the estimation is running
	boolean started;

	public ComplementaryFilter() {
		// JW: if you need to start the complementary filter -> change to true
		// Then after starting application you need to start the Inertial
		// sensors by "Start inertial sensors"
		started = true;
	}

	// Start complementary filter estimation
	public void start() {
		started = true;
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
		// JW: TODO
		return null;
	}

	// / Information from gyroscopes (feel free to change the method's name):
	// float [] gyro = [gyroX, gyroY, gyroZ] in angles/s,
	// float dt = (time from last gyro update in seconds)
	public void gyroscopeUpdate(float[] gyro, float dt) {
		Log.d(TAG, String.format(
				"gyroscopeUpdate: gyro=[%f %f %f] and dt=%f!\n", gyro[0],
				gyro[1], gyro[2], dt));
		// JW: TODO
	}

	// / Information from accelerometers/magnetometers (feel free to change the
	// method's name):
	// float [] quaternion = [quaternionW, quaternionX, quaternionY,
	// quaternionZ]
	//
	public void magAccUpdate(float[] quaternion) {
		Log.d(TAG, String.format("magAccUpdate: quat=[%f %f %f %f]\n",
				quaternion[0], quaternion[1], quaternion[2], quaternion[3]));
		// JW: TODO
	}

}
