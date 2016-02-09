package org.dg.inertialSensors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.dg.openAIL.ConfigurationReader.Parameters;

import android.R.bool;
import android.content.MutableContextWrapper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

public class InertialSensors {
	private static final String moduleLogName = "InertialSensors.java";
	
	// Main Android handler
	SensorManager sensorManager;

	// Streams to save data
	private enum ActiveStreamNames {
		ACCELEROMETER, GYROSCOPE, MAGNETOMETER, ACCELEROMETER_WITHOUT_GRAVITY, ORIENTATION_ANDROID, ORIENTATION_ANDROID_EULER, ORIENTATION_AEKF,
		ORIENTATION_AEKF_EULER, ORIENTATION_COMPLEMENTARY, ORIENTATION_COMPLEMENTARY_EULER, PRESSURE;
	}
	boolean save2file = false;
	boolean activeStreams[];
	PrintStream accStream, gyroStream, magStream, accwogStream, orientAndroidStream, orientAndroidEulerStream,
			myOrientAEKFStream, myOrientAEKFEulerStream, myOrientComplementaryStream, myOrientComplementaryEulerStream, pressureStream;

	// Starting timestamp and current timestamp
	long timestampStart = 0, currentTimestamp;
	boolean firstNewData = true;

	// Last estimates
	private final Semaphore orientMtx = new Semaphore(1, true);
	private final Semaphore orientCompMtx = new Semaphore(1, true);
	private final Semaphore orientAndroidMtx = new Semaphore(1, true);
	private final Semaphore yawMtx = new Semaphore(1, true);
	
	private final Semaphore gyroMtx = new Semaphore(1, true);
	private final Semaphore accMtx = new Semaphore(1, true);
	
	int id = 0;
	float acc[], mag[], accwog[], orient[], orientComp[], orientAndroid[], lastAndroidQuat[];

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
	private LinkedList<Float> accStepometerWindow, accVarianceWindow, gyroVarianceWindow;
	static int gyroWindowSize = 100;
	boolean stepometerStarted = false;
	int stepometerRunningCounter = 0;
	float lastYawZ = 0.0f;
	boolean firstYawCall = true;
	Stepometer stepometer;
	
	// Testing new stepometer angle
	public enum DeviceOrientation {VERTICAL, HORIZONTAL_LEFT, HORIZONTAL_RIGHT, UNKNOWN};
	private int deviceOrientationPoll [] = new int [4];
	private int deviceOrientationIter = 0;
	private DeviceOrientation deviceOrientation = DeviceOrientation.VERTICAL;
	float stepometerAngle;
	
	
	// Acc variance
	int accVarianceRunningCounter = 0;
	final int accVarianceWindowSize = 100;
	
	// Magnetic recognition
	private final Semaphore magneticWindowMtx = new Semaphore(1, true);
	private List<Float> magneticWindow;
	int magneticRecognitionCounter = 0;
	final int magneticWindowSize = 512;
	MagneticRecognition magneticRecognition;
	
	// Barometer
	float lastBarometerValue = 0;
	BarometerProcessing barometer;
	
	// Parameters
	Parameters.InertialSensors parameters;

	public InertialSensors(SensorManager _sensorManager, Parameters.InertialSensors _parameters) {
		sensorManager = _sensorManager;
		parameters = _parameters;
		
		isStarted = false;
		acc = new float[3];
		mag = new float[3];
		accwog = new float[3];
		orient = new float[3];
		orientComp = new float[3];
		orientAndroid = new float[3];
		lastAndroidQuat = new float[4];
		performOrientationEstimation = true;
		
		accStepometerWindow = new LinkedList<Float>();
		accVarianceWindow = new LinkedList<Float>();
		gyroVarianceWindow = new LinkedList<Float>();
		stepometer = new Stepometer(parameters.stepometer);
		
		magneticWindow = new ArrayList<Float>();
		magneticRecognition = new MagneticRecognition();
		
		barometer = new BarometerProcessing();
		
		// Complementary filter
		complementaryFilterEstimation = new ComplementaryFilter(0.999325f);
		
		/// Choose which filters to save
		activeStreams = new boolean[11];
		
		activateStreamBasedOnParams();	
		
	}

	/**
	 * 
	 */
	private void activateStreamBasedOnParams() {
		// Basic sensors
		activeStreams[ActiveStreamNames.ACCELEROMETER.ordinal()] = parameters.record.accelerometer;
		activeStreams[ActiveStreamNames.GYROSCOPE.ordinal()] = parameters.record.gyroscope;
		activeStreams[ActiveStreamNames.MAGNETOMETER.ordinal()] = parameters.record.magnetometer;
		activeStreams[ActiveStreamNames.ACCELEROMETER_WITHOUT_GRAVITY.ordinal()] = parameters.record.accelerometerWithoutGravity;
		activeStreams[ActiveStreamNames.PRESSURE.ordinal()] = parameters.record.barometer;
		
		// Android estimation
		activeStreams[ActiveStreamNames.ORIENTATION_ANDROID.ordinal()] = parameters.record.orientationAndroid;
		activeStreams[ActiveStreamNames.ORIENTATION_ANDROID_EULER.ordinal()] = parameters.record.orientationAndroidEuler;
		
		// AEKF
		activeStreams[ActiveStreamNames.ORIENTATION_AEKF.ordinal()] = parameters.record.orientationAEKF;
		activeStreams[ActiveStreamNames.ORIENTATION_AEKF_EULER.ordinal()] = parameters.record.orientationAEKFEuler;
		
		// CF
		activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY.ordinal()] = parameters.record.orientationCF;
		activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY_EULER.ordinal()] = parameters.record.orientationCFEuler;
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

	public float getStepometerLastDetectedFrequency() {
		return stepometer.getLastFoundFreq();
	}

	public float getStepometerCoveredStepDistance() {
		return stepometer.getCoveredStepDistance();
	}
	
	public float getStepometerStepDistance() {
		return stepometer.getStepDistance();
	}

	public float getStepometerNumberOfSteps() {
		return stepometer.getDetectedNumberOfSteps();
	}
	
	
	public float getStepometerAngle() {
		return stepometerAngle;
	}
	
	/**
	 * Returns Yaw (Z-axis) in degrees
	 */
	public float getYawForStepometer() {
		try {
			yawMtx.acquire();
			
			float yawZ = 0.0f;
			float firstCallBias = 0.0f;
			if ( parameters.verticalOrientation == true ) {
				yawZ = stepometerAngle;
				firstCallBias = (float) parameters.priorMapStepometerBiasVertical;
			}
			else {
				yawZ = stepometerAngle;
				firstCallBias = (float) parameters.priorMapStepometerBiasHorizontal;
			}
			yawMtx.release();
			
			// In any case we compute the difference in orientations
			float deltaYaw = yawZ - lastYawZ;
			lastYawZ = yawZ;
			
			// In first case, the diff is equal to global orientation corrected by map bias
			if ( firstYawCall) {
				Log.d(moduleLogName, "deltaYaw = " + deltaYaw + " firstCallBias = " + firstCallBias);
				firstYawCall = false;
				return deltaYaw + firstCallBias;
			}
			return deltaYaw;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0.0f;
	}
	
	public float getGlobalYaw() {
		try {
			yawMtx.acquire();
			float yawZ = orientAndroid[2];
			yawMtx.release();
			return yawZ;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0.0f;
	}
	
	public float getAccVariance() {
		float val = 0.0f;
		try {
			accMtx.acquire();
			val = varList(accVarianceWindow);
			accMtx.release();
		} catch (Exception e) { }
		
		return val;
	}
	
	public float getGyroVariance() {
		
		float val = 0.0f;
		try {
			gyroMtx.acquire();
			val = varList(gyroVarianceWindow);
			gyroMtx.release();
		} catch (InterruptedException e) {
		}
		
		return val;
	}
		
	
	public int getDeviceOrientation() {
		return deviceOrientation.ordinal();
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

		init();
		
		
		
		initPhysicalSensors();

	}
	
	public void startPlayback() {
		init();
	}

	/**
	 * 
	 */
	private void init() {
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
		firstNewData = true;
	}

	/**
	 * 
	 */
	private void initPhysicalSensors() {
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
		deviceOrientationIter = 0;
		deviceOrientationPoll = new int[4]; 

		sensorManager.unregisterListener(sensorEventListener);

		if (performOrientationEstimation) {
			orientationEstimator.destroy();
			orientationEstimator = null;
			
			complementaryFilterEstimation.stop();
		}

		if (save2file)
			closeStreams();
	}

	public void setStartTime() {
		timestampStart = System.nanoTime();
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


	public void recordAll(boolean value) {
		save2file = true;
		if(value)
		{
			// Basic sensors
			activeStreams[ActiveStreamNames.ACCELEROMETER.ordinal()] = true;
			activeStreams[ActiveStreamNames.GYROSCOPE.ordinal()] = true;
			activeStreams[ActiveStreamNames.MAGNETOMETER.ordinal()] = true;
			activeStreams[ActiveStreamNames.ACCELEROMETER_WITHOUT_GRAVITY.ordinal()] = false;
			activeStreams[ActiveStreamNames.PRESSURE.ordinal()] = true;
			
			// Android estimation
			activeStreams[ActiveStreamNames.ORIENTATION_ANDROID.ordinal()] = true;
			activeStreams[ActiveStreamNames.ORIENTATION_ANDROID_EULER.ordinal()] = false;
			
			// AEKF
			activeStreams[ActiveStreamNames.ORIENTATION_AEKF.ordinal()] = true;
			activeStreams[ActiveStreamNames.ORIENTATION_AEKF_EULER.ordinal()] = false;
			
			// CF
			activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY.ordinal()] = false;
			activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY_EULER.ordinal()] = false;
		}
		else
		{
			activateStreamBasedOnParams();
		}
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
				+ "/OpenAIL");

		if (!folder.exists()) {
			folder.mkdir();
		}

		File dirInertialSensors = new File(folder.getAbsolutePath() + "/inertialSensors");
		if (!dirInertialSensors.exists()) {
			dirInertialSensors.mkdirs();
		}
		
		File dirRawData = new File(folder.getAbsolutePath() + "/rawData");
		if (!dirRawData.exists()) {
			dirRawData.mkdirs();
		}

		if ( activeStreams[ActiveStreamNames.ACCELEROMETER.ordinal()] ) {
			String fileName = dirRawData.toString() + "/acc.log";
			FileOutputStream faccStream = new FileOutputStream(fileName);
			accStream = new PrintStream(faccStream);
		}

		if ( activeStreams[ActiveStreamNames.GYROSCOPE.ordinal()] ) {
			String fileName = dirRawData.toString() + "/gyro.log";
			FileOutputStream fgyroStream = new FileOutputStream(fileName);
			gyroStream = new PrintStream(fgyroStream);
		}

		if ( activeStreams[ActiveStreamNames.MAGNETOMETER.ordinal()] ) {
			String fileName = dirRawData.toString() + "/mag.log";
			FileOutputStream fmagStream = new FileOutputStream(fileName);
			magStream = new PrintStream(fmagStream);
		}

		if ( activeStreams[ActiveStreamNames.ACCELEROMETER_WITHOUT_GRAVITY.ordinal()] ) {
			String fileName = dirRawData.toString() + "/accwog.log";
			FileOutputStream faccwogStream = new FileOutputStream(fileName);
			accwogStream = new PrintStream(faccwogStream);
		}

		if ( activeStreams[ActiveStreamNames.ORIENTATION_ANDROID.ordinal()] ) {
			String fileName = dirRawData.toString() + "/orientAndroid.log";
			FileOutputStream forientAndroidStream = new FileOutputStream(fileName);
			orientAndroidStream = new PrintStream(forientAndroidStream);
		}
		
		if ( activeStreams[ActiveStreamNames.ORIENTATION_ANDROID_EULER.ordinal()] ) {
			String fileName = dirRawData.toString() + "/orientAndroidEuler.log";
			FileOutputStream forientAndroidEulerStream = new FileOutputStream(fileName);
			orientAndroidEulerStream = new PrintStream(forientAndroidEulerStream);
		}

		if (performOrientationEstimation) {
			if ( activeStreams[ActiveStreamNames.ORIENTATION_AEKF.ordinal()] ) {
				String fileName = dirInertialSensors.toString() + "/myOrientAEKF.log";
				FileOutputStream fmyOrientStream = new FileOutputStream(fileName);
				myOrientAEKFStream = new PrintStream(fmyOrientStream);
			}

			if (activeStreams[ActiveStreamNames.ORIENTATION_AEKF_EULER.ordinal()]) {
				String fileName = dirInertialSensors.toString() + "/myOrientAEKFEuler.log";
				FileOutputStream fmyOrientEulerStream = new FileOutputStream(
						fileName);
				myOrientAEKFEulerStream = new PrintStream(fmyOrientEulerStream);
			}
			
			if ( activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY.ordinal()] ) {
				String fileName = dirInertialSensors.toString() + "/myOrientComplementary.log";
				FileOutputStream fmyOrientComplementaryStream = new FileOutputStream(fileName);
				myOrientComplementaryStream = new PrintStream(fmyOrientComplementaryStream);
			}

			if ( activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY_EULER.ordinal()] ) {
				String fileName = dirInertialSensors.toString() + "/myOrientComplementaryEuler.log";
				FileOutputStream fmyOrientComplementaryEulerStream = new FileOutputStream(
						fileName);
				myOrientComplementaryEulerStream = new PrintStream(fmyOrientComplementaryEulerStream);
			}
	
		}
		
		if ( activeStreams[ActiveStreamNames.PRESSURE.ordinal()] ) {
			String fileName = dirRawData.toString() + "/pressure.log";
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


	public void playback(MySensorEvent event) {
		
		
		
		processNewData(event);
	}
	
	// Listener pushing the data into the files
	private SensorEventListener sensorEventListener = new SensorEventListener() {

		// We do nothing
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		// On new data
		public void onSensorChanged(SensorEvent event) {

			MySensorEvent myEvent = new MySensorEvent(event.timestamp, event.sensor.getType(), event.values);
			processNewData(myEvent);
		}

		
	};
	
	/**
	 * @param event
	 */
	private void processNewData(MySensorEvent event) {
		currentTimestamp = event.timestamp;
		if (firstNewData) {
			firstNewData = false;
			timestampStart = event.timestamp - (System.nanoTime() - timestampStart);
		}

		if (event.sensorType == Sensor.TYPE_ACCELEROMETER) {

			processNewAccelerometerData(event);

		} else if (event.sensorType == Sensor.TYPE_GYROSCOPE) {
			
			processNewGyroscopeData(event);

		} else if (event.sensorType == Sensor.TYPE_MAGNETIC_FIELD) {

			processNewMagneticData(event);

		} else if (event.sensorType == Sensor.TYPE_LINEAR_ACCELERATION) {
			
			if (save2file && activeStreams[ActiveStreamNames.ACCELEROMETER_WITHOUT_GRAVITY.ordinal()])
				saveToStream(accwogStream, getTimestamp(), event.values);

		} else if (event.sensorType == Sensor.TYPE_ROTATION_VECTOR) {

			processNewOrientationData(event);

		} else if (event.sensorType == Sensor.TYPE_PRESSURE) {
			if (save2file && activeStreams[ActiveStreamNames.PRESSURE.ordinal()] )
				saveToStream(pressureStream, getTimestamp(), event.values);
			processNewPressureData(event);
	
		}
	}

	/**
	 * Computes the height in meters 
	 */
	private void processNewPressureData(MySensorEvent event) {
		lastBarometerValue = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);
	}

	/**
	 * Updates orientation filters
	 */
	private void processNewOrientationData(MySensorEvent event) {	
		
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
		
		if (save2file && activeStreams[ActiveStreamNames.ORIENTATION_ANDROID.ordinal()])
			saveToStream(orientAndroidStream, getTimestamp(), quaternion);
		if (save2file && activeStreams[ActiveStreamNames.ORIENTATION_ANDROID_EULER.ordinal()])
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
				
				if (save2file && activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY.ordinal()])
					saveToStream(myOrientComplementaryStream, getTimestamp(), orientCompQuat);
				if (save2file && activeStreams[ActiveStreamNames.ORIENTATION_COMPLEMENTARY_EULER.ordinal()])
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

			if (save2file && activeStreams[ActiveStreamNames.ORIENTATION_AEKF.ordinal()])
				saveToStream(myOrientAEKFStream, getTimestamp(), orientAEKFQuat);
			if (save2file && activeStreams[ActiveStreamNames.ORIENTATION_AEKF_EULER.ordinal()])
				saveToStream(myOrientAEKFEulerStream, getTimestamp(),
						orient);
		
		}
		
		
		// TEST
		// Testing new stepometer angle

		float[] R = new float[9];
		SensorManager.getRotationMatrixFromVector(R, event.values);
//		if (deviceOrientation == deviceOrientation.VERTICAL)
//			stepometerAngle = (float) (Math.atan2(R[3], R[0]) * 180.0d / Math.PI);
//		else if (deviceOrientation == deviceOrientation.HORIZONTAL_LEFT)
//			stepometerAngle = (float) (Math.atan2(-R[4], -R[1]) * 180.0d / Math.PI);
//		else if (deviceOrientation == DeviceOrientation.HORIZONTAL_RIGHT)
//			stepometerAngle = (float) (Math.atan2(R[4], R[1]) * 180.0d / Math.PI);
//		else if (deviceOrientation == DeviceOrientation.UNKNOWN)
//			stepometerAngle = (float) (90.0f - Math.atan2(-R[5], R[2]) * 180.0d / Math.PI);

		stepometerAngle = (float) (90.0f - Math.atan2(-R[5], R[2]) * 180.0d / Math.PI);
		
//		double Zval = Math.sqrt(R[5]*R[5] + R[2]*R[2]);
//		double Yval = Math.sqrt(R[4]*R[4] + R[1]*R[1]);
//		
//		double stepometerAngle2 = (float) (90.0f - Math.atan2(-R[4], R[1]) * 180.0d / Math.PI);
//		Log.d(moduleLogName, "Zval =" + Zval + " Yval=" + Yval +  " || stepomterAngle=" + stepometerAngle + " step2=" +stepometerAngle2);
	}



	/**
	 * @param event
	 */
	private void processNewMagneticData(MySensorEvent event) {
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

		if (save2file && activeStreams[ActiveStreamNames.MAGNETOMETER.ordinal()])
			saveToStream(magStream, getTimestamp(), mag);
	}

	/**
	 * @param event
	 */
	private void processNewGyroscopeData(MySensorEvent event) {
		float [] gyro = new float [3];
		gyro[0] = event.values[0];
		gyro[1] = event.values[1];
		gyro[2] = event.values[2];
		
		// Gyro window
		try {
			gyroMtx.acquire();
			gyroVarianceWindow.add((float) Math.sqrt(gyro[0]*gyro[0]+gyro[1]*gyro[1]+gyro[2]*gyro[2]));
			if ( gyroVarianceWindow.size() > gyroWindowSize)
			{
				gyroVarianceWindow.removeFirst();
			}
			gyroMtx.release();
		} catch (Exception e) {};
		
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

		if (save2file && activeStreams[ActiveStreamNames.GYROSCOPE.ordinal()])
			saveToStream(gyroStream, getTimestamp(), gyro);
	}

	/**
	 * @param event
	 */
	private void processNewAccelerometerData(MySensorEvent event) {
		acc[0] = event.values[0];
		acc[1] = event.values[1];
		acc[2] = event.values[2];
		
		// select device orientation type
		if ( deviceOrientationIter < 50) {
			if ( acc[1] > Math.abs(acc[0]) && acc[1] > Math.abs(acc[2]) )
				deviceOrientationPoll[DeviceOrientation.VERTICAL.ordinal()]++;
			else if ( acc[0] > Math.abs(acc[1]) && acc[0] > Math.abs(acc[2]))
				deviceOrientationPoll[DeviceOrientation.HORIZONTAL_LEFT.ordinal()]++;
			else if ( acc[0] < Math.abs(acc[1]) && acc[0] < Math.abs(acc[2]))
				deviceOrientationPoll[DeviceOrientation.HORIZONTAL_RIGHT.ordinal()]++;
			else
				deviceOrientationPoll[DeviceOrientation.UNKNOWN.ordinal()]++;
			
			deviceOrientationIter++;
		}
		else if ( deviceOrientationIter == 50) {
			
			int max = deviceOrientationPoll[0];
			int index = 0;
			for (int i=1;i<4;i++) {
				if ( deviceOrientationPoll[i] > max) {
					max = deviceOrientationPoll[i];
					index = i;
				}
			}
			
			deviceOrientation = DeviceOrientation.values()[index];
			
			//TESTING
			//deviceOrientation = DeviceOrientation.UNKNOWN;
		}

		float accVal = (float) Math.sqrt(acc[0] * acc[0] + acc[1]
				* acc[1] + acc[2] * acc[2]);
		
		try {
			accMtx.acquire();
				accVarianceWindow.add(Float.valueOf(accVal));
			accMtx.release();
			
			accStepometerWindow.add(Float.valueOf(accVal));
			
		} catch (Exception e) {};
		
		
		// Remove element if too long
//		if (accVarianceWindow.size() > accVarianceWindowSize) {
//			accVarianceWindow.removeFirst();
//		}
		if (accStepometerWindow.size() > parameters.stepometer.windowSize) {
			accStepometerWindow.removeFirst();
		}

		if (save2file && activeStreams[ActiveStreamNames.ACCELEROMETER.ordinal()])
			saveToStream(accStream, getTimestamp(), acc);
		
		if (stepometerStarted && stepometerRunningCounter > 200)
		{
			Log.d(moduleLogName, "Compare sizes: " + accStepometerWindow.size() + " " + parameters.stepometer.windowSize);
			if ( accStepometerWindow.size() == parameters.stepometer.windowSize)
			{
				
				 // Copying from list to float array, so we can process in new thread
				 int accWindowSize = accStepometerWindow.size();
				 float [] accWindowFloat = new float[accWindowSize];
				 for (int i=0;i<accWindowSize;i++)
				 {
					accWindowFloat[i] = accStepometerWindow.get(i);
				 }
				 stepometer.setAccWindow(accWindowFloat, accWindowSize);
				
				 // Run stepometer
				 new Thread(stepometer).start();

			}
			stepometerRunningCounter = 0;
		}
		stepometerRunningCounter++;
		accVarianceRunningCounter++;
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
	 */
	private void saveToStream(PrintStream stream, Long timestamp,
			float[] values) {

		stream.print(Long.toString(timestamp));
		for (int i = 0; i < values.length; i++) {
			stream.print(" " + Float.toString(values[i]));
		}
		stream.print(System.getProperty("line.separator"));
	}
	
	private float avgList(LinkedList<Float> values) {
		float s = 0;
		for (Float x : values)
			s+=x;
		return s/values.size();
	}
	
	private float varList(LinkedList<Float> values) {
		float sumDiffsSquared = 0.0f;
		float avg = avgList(values);
		for (Float value : values) {
			double diff = value - avg;
			diff *= diff;
			sumDiffsSquared += diff;
		}
		return sumDiffsSquared / (values.size() - 1);
	}
	
	
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
