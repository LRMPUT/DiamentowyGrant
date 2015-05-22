package org.dg.inertialSensors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.content.MutableContextWrapper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

public class InertialSensors {
	// Parameters
	static final int verboseLevel = 0;

	// Main Android handler
	SensorManager sensorManager;

	// Streams to save data
	public enum activeStream {
		ACCELEROMETER, GYROSCOPE, MAGNETOMETER, ACCELEROMETER_WITHOUT_GRAVITY, ORIENTATION_ANDROID, ORIENTATION_ANDROID_EULER, ORIENTATION_AEKF,
		ORIENTATION_AEKF_EULER, ORIENTATION_COMPLEMENTARY, ORIENTATION_COMPLEMENTARY_EULER, PRESSURE;
	}
	boolean save2file = false;
	boolean activeStreams[];
	PrintStream accStream, gyroStream, magStream, accwogStream, orientAndroidStream, orientAndroidEulerStream,
			myOrientAEKFStream, myOrientAEKFEulerStream, myOrientComplementaryStream, myOrientComplementaryEulerStream, pressureStream;

	// Starting timestamp and current timestamp
	long timestampStart, currentTimestamp;

	// Last estimates
	private final Semaphore orientMtx = new Semaphore(1, true);
	private final Semaphore orientCompMtx = new Semaphore(1, true);
	private final Semaphore orientAndroidMtx = new Semaphore(1, true);
	private final Semaphore yawMtx = new Semaphore(1, true);
	
	int id = 0;
	float acc[], mag[], accwog[], gyro[], orient[], orientComp[], orientAndroid[], lastAndroidQuat[];

	// isRunning
	boolean isStarted;

	// AEKF
	boolean performOrientationEstimation = true;
	long lastOrientationEstimationTimestamp;
	AHRSModule orientationEstimator = null;
	
	// Complementary filter
	boolean runComplementaryFilter = false;
	ComplementaryFilter complementaryFilterEstimation = null;

	// Stepometer
	private List<Float> accWindow;
	boolean stepometerStarted = false;
	int stepometerRunningCounter = 0;
	final int stepometerWindowSize = 1024;
	float lastYawZ = 0.0f;
	boolean firstYawCall = true;
	Stepometer stepometer;
	
	// Magnetic recognition
	private final Semaphore magneticWindowMtx = new Semaphore(1, true);
	private List<Float> magneticWindow;
	int magneticRecognitionCounter = 0;
	final int magneticWindowSize = 512;
	MagneticRecognition magneticRecognition;
	
	// Barometer
	float lastBarometerValue = 0;
	BarometerProcessing barometer;

	public InertialSensors(SensorManager _sensorManager) {
		sensorManager = _sensorManager;
		isStarted = false;
		acc = new float[3];
		mag = new float[3];
		accwog = new float[3];
		gyro = new float[3];
		orient = new float[3];
		orientComp = new float[3];
		orientAndroid = new float[3];
		lastAndroidQuat = new float[4];
		performOrientationEstimation = true;
		accWindow = new ArrayList<Float>();
		stepometer = new Stepometer();
		
		magneticWindow = new ArrayList<Float>();
		magneticRecognition = new MagneticRecognition();
		
		barometer = new BarometerProcessing();
		
		// Complementary filter
		complementaryFilterEstimation = new ComplementaryFilter(0.999325f);
		
		// Choose which filters to save
		activeStreams = new boolean[11];
		activeStreams[activeStream.ORIENTATION_AEKF.ordinal()] = true;
		activeStreams[activeStream.ORIENTATION_AEKF_EULER.ordinal()] = true;
		activeStreams[activeStream.ORIENTATION_ANDROID.ordinal()] = true;
		activeStreams[activeStream.ORIENTATION_ANDROID_EULER.ordinal()] = true;
		activeStreams[activeStream.GYROSCOPE.ordinal()] = true;
		
	}

	public InertialSensors save2file(boolean _save2file) {
		this.save2file = _save2file;
		return this;
	}

	public InertialSensors performOrientationEstimation(
			boolean _performOrientationEstimation) {
		this.performOrientationEstimation = _performOrientationEstimation;
		return this;
	}

	public InertialSensors id(int _id) {
		this.id = _id;
		return this;
	}

	public boolean getState() {
		return isStarted;
	}

	
	// Stepometer
	public void startStepometer() {
		stepometerStarted = true;

	}

	public void stopStepometer() {
		stepometerStarted = false;
	}
	
	public boolean isStepometerStarted() {
		return stepometerStarted;
	}

	public float getLastDetectedFrequency() {
		return stepometer.getLastFoundFreq();
	}

	public float getCovertedStepDistance() {
		return stepometer.getCoveredStepDistance();
	}
	
	public float getGraphStepDistance() {
		return stepometer.getGraphStepDistance();
	}

	public float getDetectedNumberOfSteps() {
		return stepometer.getDetectedNumberOfSteps();
	}
	
	public float getYawForStepometer() {
		try {
			yawMtx.acquire();
			float yawZ = orientAndroid[2];
			yawMtx.release();
			float deltaYaw = yawZ - lastYawZ;
			lastYawZ = yawZ;
			
			if ( firstYawCall) {
				firstYawCall = false;
				return 0.0f;
			}
			return deltaYaw;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0.0f;
	}
		
	
	// Magnetic recognition
	public int recognizePlaceBasedOnMagneticScan()
	{
		// TODO!
		return -1;
		
//		try {
//			magneticWindowMtx.acquire();
//			List<Float> copy = new ArrayList<Float>(magneticWindow.subList(0, 512));
//			magneticWindowMtx.release();
//			
//			return magneticRecognition.recognizePlace(copy);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//			return -1;
//		}
	}
	
	public void addMagneticRecognitionPlace()
	{
		
		try {
			magneticWindowMtx.acquire();
			List<Float> copy = new ArrayList<Float>(magneticWindow.subList(0, 512));
			magneticWindowMtx.release();
			
			magneticRecognition.addPlace(copy);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public int getSizeOfPlaceDatabase()
	{
		return magneticRecognition.getSizeOfPlaceDatabase();
	}
	
	
	// Barometer
	public void startBarometerProcessing() {
		barometer.start(lastBarometerValue);
	}
	
	public void stopBarometerProcessing() {
		barometer.stop();
	}
	
	public boolean isBarometerProcessingStarted() {
		return barometer.isStarted();
	}
	
	public float getEstimatedHeight() {
		return barometer.getEstimateHeight();
	}
	
	public int getCurrentFloor() {
		barometer.setCurrentHeight(lastBarometerValue);
		return barometer.getCurrentFloor();
	}
	
	

	// Start
	public void start() {

		lastOrientationEstimationTimestamp = -1;
		
		// Initialize out orientation estimation
		if (performOrientationEstimation)
		{
			orientationEstimator = new AHRSModule();
		}
		
		if (runComplementaryFilter) {
			complementaryFilterEstimation.start();
		}

		isStarted = true;
		timestampStart = 0;
		if (save2file) {
			try {
				openStreamsToFiles(id);
				id++;
			} catch (FileNotFoundException e) {
				save2file = false;
			}
		}

		// Sensor handlers
		Sensor accelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		Sensor magnetic = sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		Sensor acc_wo_gravity = sensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Sensor orientation = sensorManager
				.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

		// Starting listeners
		sensorManager.registerListener(sensorEventListener, accelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, gyro,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, magnetic,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, acc_wo_gravity,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, orientation,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, pressureSensor,
				SensorManager.SENSOR_DELAY_FASTEST);

	}

	// Stop the processing
	public void stop() {
		isStarted = false;
		timestampStart = 0;

		sensorManager.unregisterListener(sensorEventListener);

		if (performOrientationEstimation) {
			orientationEstimator.destroy();
			orientationEstimator = null;
			
			complementaryFilterEstimation.stop();
		}

		if (save2file)
			closeStreams();
	}

	public float[] getCurrentMagnetometer() {

		return mag;
	}

	public float[] getCurrentAcc() {
		return acc;
	}

	public float[] getCurrentAEKFOrient() {
		float[] orientToReturn = null;
		try {
			orientMtx.acquire();
			orientToReturn = orient.clone();
			orientMtx.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return orientToReturn;
	}
	
	public float[] getCurrentComplementaryOrient() {
		float[] orientToReturn = null;
		try {
			orientCompMtx.acquire();
			orientToReturn = orientComp.clone();
			orientCompMtx.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return orientToReturn;
	}
	
	public float[] getCurrentAndroidOrient() {
		float[] orientToReturn = null;
		try {
			orientAndroidMtx.acquire();
			orientToReturn = orientAndroid.clone();
			orientAndroidMtx.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return orientToReturn;
	}


	public long getTimestamp() {
		return (currentTimestamp - timestampStart);
	}

	private void openStreamsToFiles(int id) throws FileNotFoundException {

		accStream = null;
		gyroStream = null;
		magStream = null;
		accwogStream = null;
		orientAndroidStream = null;
		orientAndroidEulerStream = null;
		myOrientAEKFStream = null;
		myOrientAEKFEulerStream = null;
		myOrientComplementaryStream = null;
		myOrientComplementaryEulerStream = null;
		pressureStream = null;

		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/DG");

		if (!folder.exists()) {
			folder.mkdir();
		}

		File dir = new File(String.format(
				Environment.getExternalStorageDirectory() + "/DG/inertial"));
		if (!dir.exists()) {
			dir.mkdirs();
		}

		if ( activeStreams[activeStream.ACCELEROMETER.ordinal()] ) {
			String fileName = dir.toString() + "/acc.log";
			FileOutputStream faccStream = new FileOutputStream(fileName);
			accStream = new PrintStream(faccStream);
		}

		if ( activeStreams[activeStream.GYROSCOPE.ordinal()] ) {
			String fileName = dir.toString() + "/gyro.log";
			FileOutputStream fgyroStream = new FileOutputStream(fileName);
			gyroStream = new PrintStream(fgyroStream);
		}

		if ( activeStreams[activeStream.MAGNETOMETER.ordinal()] ) {
			String fileName = dir.toString() + "/mag.log";
			FileOutputStream fmagStream = new FileOutputStream(fileName);
			magStream = new PrintStream(fmagStream);
		}

		if ( activeStreams[activeStream.ACCELEROMETER_WITHOUT_GRAVITY.ordinal()] ) {
			String fileName = dir.toString() + "/accwog.log";
			FileOutputStream faccwogStream = new FileOutputStream(fileName);
			accwogStream = new PrintStream(faccwogStream);
		}

		if ( activeStreams[activeStream.ORIENTATION_ANDROID.ordinal()] ) {
			String fileName = dir.toString() + "/orientAndroid.log";
			FileOutputStream forientAndroidStream = new FileOutputStream(fileName);
			orientAndroidStream = new PrintStream(forientAndroidStream);
		}
		
		if ( activeStreams[activeStream.ORIENTATION_ANDROID_EULER.ordinal()] ) {
			String fileName = dir.toString() + "/orientAndroidEuler.log";
			FileOutputStream forientAndroidEulerStream = new FileOutputStream(fileName);
			orientAndroidEulerStream = new PrintStream(forientAndroidEulerStream);
		}

		if (performOrientationEstimation) {
			if ( activeStreams[activeStream.ORIENTATION_AEKF.ordinal()] ) {
				String fileName = dir.toString() + "/myOrientAEKF.log";
				FileOutputStream fmyOrientStream = new FileOutputStream(fileName);
				myOrientAEKFStream = new PrintStream(fmyOrientStream);
			}

			if (activeStreams[activeStream.ORIENTATION_AEKF_EULER.ordinal()]) {
				String fileName = dir.toString() + "/myOrientAEKFEuler.log";
				FileOutputStream fmyOrientEulerStream = new FileOutputStream(
						fileName);
				myOrientAEKFEulerStream = new PrintStream(fmyOrientEulerStream);
			}
			
			if ( activeStreams[activeStream.ORIENTATION_COMPLEMENTARY.ordinal()] ) {
				String fileName = dir.toString() + "/myOrientComplementary.log";
				FileOutputStream fmyOrientComplementaryStream = new FileOutputStream(fileName);
				myOrientComplementaryStream = new PrintStream(fmyOrientComplementaryStream);
			}

			if ( activeStreams[activeStream.ORIENTATION_COMPLEMENTARY_EULER.ordinal()] ) {
				String fileName = dir.toString() + "/myOrientComplementaryEuler.log";
				FileOutputStream fmyOrientComplementaryEulerStream = new FileOutputStream(
						fileName);
				myOrientComplementaryEulerStream = new PrintStream(fmyOrientComplementaryEulerStream);
			}
	
		}
		
		if ( activeStreams[activeStream.PRESSURE.ordinal()] ) {
			String fileName = dir.toString() + "/pressure.log";
			FileOutputStream fpressureStream = new FileOutputStream(fileName);
			pressureStream = new PrintStream(fpressureStream);
		}

	}

	// Closing the data streams
	private void closeStreams() {
		if (accStream != null)
			accStream.close();
		if (gyroStream != null)
			gyroStream.close();
		if (magStream != null)
			magStream.close();
		if (accwogStream != null)
			accwogStream.close();
		if (orientAndroidStream != null)
			orientAndroidStream.close();
		if (orientAndroidEulerStream != null)
			orientAndroidEulerStream.close();

		if (performOrientationEstimation) {
			if (myOrientAEKFStream != null)
				myOrientAEKFStream.close();
			if (myOrientAEKFEulerStream != null)
				myOrientAEKFEulerStream.close();

			if (myOrientComplementaryStream != null)
				myOrientComplementaryStream.close();
			if (myOrientComplementaryEulerStream != null)
				myOrientComplementaryEulerStream.close();
		}
		
		if (pressureStream != null)
			pressureStream.close();
	}


	// Listener pushing the data into the files
	private SensorEventListener sensorEventListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void onSensorChanged(SensorEvent event) {


			currentTimestamp = event.timestamp;
			if (timestampStart == 0) {
				timestampStart = event.timestamp;
			}

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

				processNewAccelerometerData(event);

			} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
				
				processNewGyroscopeData(event);

			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

				processNewMagneticData(event);

			} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				
				if (save2file && activeStreams[activeStream.ACCELEROMETER_WITHOUT_GRAVITY.ordinal()])
					saveToStream(accwogStream, getTimestamp(), event.values);

			} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

				processNewOrientationData(event);

			} else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
				
				processNewPressureData(event);
		
			}
		}

		/**
		 * @param event
		 */
		private void processNewPressureData(SensorEvent event) {
			event.values[2] = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);
			lastBarometerValue = event.values[1];
			
			if (save2file && activeStreams[activeStream.PRESSURE.ordinal()] )
				saveToStream(pressureStream, getTimestamp(), event.values);
		}

		/**
		 * @param event
		 */
		private void processNewOrientationData(SensorEvent event) {
			float[] quaternion = new float[4];
			SensorManager.getQuaternionFromVector(quaternion,
					event.values);
			
			// Check if need to change sign
			float noChangeSign = RMSE(quaternion, lastAndroidQuat);
			float changeSign = RMSE(quaternion, minus(lastAndroidQuat));
			if (noChangeSign>changeSign) {
				quaternion = minus(quaternion);
			}
			lastAndroidQuat = quaternion;
			
			try {
				orientAndroidMtx.acquire();
				orientAndroid[0] = computeEulerRollX(quaternion);
				orientAndroid[1] = computeEulerPitchY(quaternion);
				
				yawMtx.acquire();
				orientAndroid[2] = computeEulerYawZ(quaternion);
				yawMtx.release();
				
				orientAndroidMtx.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (save2file && activeStreams[activeStream.ORIENTATION_ANDROID.ordinal()])
				saveToStream(orientAndroidStream, getTimestamp(), quaternion);
			if (save2file && activeStreams[activeStream.ORIENTATION_ANDROID_EULER.ordinal()])
				saveToStream(orientAndroidEulerStream, getTimestamp(), orientAndroid);

			if (performOrientationEstimation) {
				
				// Complementary filter
				if (complementaryFilterEstimation.getState()) {
					
					
					complementaryFilterEstimation.magAccUpdate(quaternion);
					
					float [] orientCompQuat = complementaryFilterEstimation.getEstimate();
						
					try {
						orientCompMtx.acquire();
						orientComp[0] = computeEulerRollX(orientCompQuat);
						orientComp[1] = computeEulerPitchY(orientCompQuat);
						orientComp[2] = computeEulerYawZ(orientCompQuat);
						orientCompMtx.release();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					if (save2file && activeStreams[activeStream.ORIENTATION_COMPLEMENTARY.ordinal()])
						saveToStream(myOrientComplementaryStream, getTimestamp(), orientCompQuat);
					if (save2file && activeStreams[activeStream.ORIENTATION_COMPLEMENTARY_EULER.ordinal()])
						saveToStream(myOrientComplementaryEulerStream, getTimestamp(), orientComp);
					

				}
				
		
				orientationEstimator.correct(quaternion[0], quaternion[1],
						quaternion[2], quaternion[3]);
				
				
				// Getting the estimate from our AEKF
				float [] orientAEKFQuat = orientationEstimator.getEstimate();
				
				try {
					orientMtx.acquire();
					orient[0] = computeEulerRollX(orientAEKFQuat);
					orient[1] = computeEulerPitchY(orientAEKFQuat);
					orient[2] = computeEulerYawZ(orientAEKFQuat);
					orientMtx.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (save2file && activeStreams[activeStream.ORIENTATION_AEKF.ordinal()])
					saveToStream(myOrientAEKFStream, getTimestamp(), orientAEKFQuat);
				if (save2file && activeStreams[activeStream.ORIENTATION_AEKF_EULER.ordinal()])
					saveToStream(myOrientAEKFEulerStream, getTimestamp(),
							orient);
			
			}
		}

		/**
		 * //http://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
		 */
		private float computeEulerYawZ(float w, float x, float y, float z) {
			return (float) (Math.atan2(2*(w*z + y*x), 1-2*(y*y + z*z))* 180.0 / Math.PI);
		}
		
		private float computeEulerYawZ(float [] orientQuat) {
			return computeEulerYawZ(orientQuat[0], orientQuat[1], orientQuat[2], orientQuat[3]);
		}

		/**
		 */
		private float computeEulerPitchY(float w, float x, float y, float z) {
			return (float) (Math.asin(2 * (w *y - x*z)) * 180.0 / Math.PI);
		}
		
		private float computeEulerPitchY(float [] orientQuat) {
			return computeEulerPitchY(orientQuat[0], orientQuat[1], orientQuat[2], orientQuat[3]);
		}
		
		

		/**
		 */
		private float computeEulerRollX(float w, float x, float y, float z) {
			return (float) (Math.atan2(2*y*z + 2 * x * w, 1 - 2 * ( x*x + y*y) ) * 180.0 / Math.PI );
		}
		
		private float computeEulerRollX(float [] orientQuat) {
			return computeEulerRollX(orientQuat[0], orientQuat[1], orientQuat[2], orientQuat[3]);
		}

		/**
		 * @param event
		 */
		private void processNewMagneticData(SensorEvent event) {
			mag[0] = event.values[0];
			mag[1] = event.values[1];
			mag[2] = event.values[2];
			
			float magneticModule = (float) Math.sqrt(Math.pow(mag[0],2) + Math.pow(mag[1],2) + Math.pow(mag[2],2));
			
			try {
				magneticWindowMtx.acquire();
				magneticWindow.add(magneticModule);
				
				if ( magneticRecognitionCounter > 200 && magneticWindow.size() > magneticWindowSize)
				{
					// Reduce list size to stepometerWindowSize
					//Log.d("Main::Activity", "Mag1 : "+ (magneticWindow.size() - magneticWindowSize) +" " + magneticWindow.size());
					magneticWindow = magneticWindow.subList(magneticWindow.size() - magneticWindowSize, magneticWindow.size());
					//Log.d("Main::Activity", "Mag2 : " + magneticWindow.size());
					
					magneticRecognitionCounter = 0;
				}
				
				magneticWindowMtx.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			magneticRecognitionCounter++;

			if (save2file && activeStreams[activeStream.MAGNETOMETER.ordinal()])
				saveToStream(magStream, getTimestamp(), mag);
		}

		/**
		 * @param event
		 */
		private void processNewGyroscopeData(SensorEvent event) {
			gyro[0] = event.values[0];
			gyro[1] = event.values[1];
			gyro[2] = event.values[2];
			
			if (performOrientationEstimation) {
			// Time from last update converted from ns to s
			final float convertNs2s = 1000000000.0f;
			
			float dt = 0.0f;
			if ( lastOrientationEstimationTimestamp >= 0)
				dt = (event.timestamp - lastOrientationEstimationTimestamp)/convertNs2s; 
			lastOrientationEstimationTimestamp = event.timestamp;
							
				// AEKF
				orientationEstimator.predict(gyro[0], gyro[1], gyro[2], dt);
				// Complementary filter
				if (complementaryFilterEstimation.getState())
					complementaryFilterEstimation.gyroscopeUpdate(gyro, dt);
			}

			if (save2file && activeStreams[activeStream.GYROSCOPE.ordinal()])
				saveToStream(gyroStream, getTimestamp(), gyro);
		}

		/**
		 * @param event
		 */
		private void processNewAccelerometerData(SensorEvent event) {
			acc[0] = event.values[0];
			acc[1] = event.values[1];
			acc[2] = event.values[2];

			float accVal = (float) Math.sqrt(acc[0] * acc[0] + acc[1]
					* acc[1] + acc[2] * acc[2]);
			accWindow.add(Float.valueOf(accVal));

			if (save2file && activeStreams[activeStream.ACCELEROMETER.ordinal()])
				saveToStream(accStream, getTimestamp(), acc);
			
			if (stepometerStarted && stepometerRunningCounter > 200)
			{
				if ( accWindow.size() > stepometerWindowSize)
				{
					 // Reduce list size to stepometerWindowSize
					 accWindow = accWindow.subList(accWindow.size() - stepometerWindowSize, accWindow.size());
					 
					 // Copying from list to float array, so we can process in new thread
					 int accWindowSize = accWindow.size();
					 float [] accWindowFloat = new float[accWindowSize];
					 for (int i=0;i<accWindowSize;i++)
					 {
						accWindowFloat[i] = accWindow.get(i);
					 }
					 stepometer.setAccWindow(accWindowFloat, accWindowSize);
					
					 // Run stepometer
					 new Thread(stepometer).start();
				}
				stepometerRunningCounter = 0;
			}
			stepometerRunningCounter = stepometerRunningCounter + 1;
		}

		/**
		 */
		private void saveToStream(PrintStream stream, Long timestamp,
				float[] values) {

			stream.print(Long.toString(timestamp));
			for (int i = 0; i < values.length; i++) {
				stream.print(" " + Float.toString(values[i]));
			}
			stream.print(System.getProperty("line.separator"));
		}
	};
	
	
	private float RMSE(float[] x, float [] y) {
		if (x.length != y.length) {
			return 99999.0f;
		}
		float val = 0.0f;
		for (int i=0;i<4;i++) {
			val += ((x[i] - y[i])*(x[i] - y[i]));
		}
		return (float) Math.sqrt(val);
	}
	
	private float [] minus(float [] x) {
		for (int i=0;i<x.length;i++)
			x[i] = -x[i];
		return x;
	}
	

}
