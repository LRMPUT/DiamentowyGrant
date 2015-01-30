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

	// Main Android handler
	SensorManager sensorManager;

	// Streams to save data
	boolean save2file = false;
	PrintStream accStream, gyroStream, magStream, accwogStream, orientStream,
			myOrientStream, myOrientEulerStream, pressureStream;

	// Starting timestamp and current timestamp
	long timestampStart, currentTimestamp;

	// Last estimates
	private final Semaphore orientMtx = new Semaphore(1, true);
	int id = 0;
	float acc[], mag[], accwog[], gyro[], orient[];

	// isRunning
	boolean isStarted;

	// Should we estimate the orientation ?
	boolean performOrientationEstimation = true;
	// Last timestamp
	long lastOrientationEstimationTimestamp;
	AHRSModule orientationEstimator = null;
	ComplementaryFilter complementaryFilterEstimation = null;

	// Stepometer
	private List<Float> accWindow;
	boolean stepometerStarted = false;
	int stepometerRunningCounter = 0;
	final int stepometerWindowSize = 1024;
	float lastYawZ = 0.0f;
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
		performOrientationEstimation = true;
		accWindow = new ArrayList<Float>();
		stepometer = new Stepometer();
		
		magneticWindow = new ArrayList<Float>();
		magneticRecognition = new MagneticRecognition();
		
		barometer = new BarometerProcessing();
		
		// Complementary filter
		complementaryFilterEstimation = new ComplementaryFilter();
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
		float yawZ = orient[2];
		float deltaYaw = yawZ - lastYawZ;
		lastYawZ = yawZ;
		return deltaYaw;
	}
	
	// Magnetic recognition
	public int recognizePlaceBasedOnMagneticScan()
	{
		try {
			magneticWindowMtx.acquire();
			List<Float> copy = new ArrayList<Float>(magneticWindow.subList(0, 512));
			magneticWindowMtx.release();
			
			return magneticRecognition.recognizePlace(copy);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
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

		// Initialize out orientation estimation
		if (performOrientationEstimation)
		{
			lastOrientationEstimationTimestamp = -1;
			orientationEstimator = new AHRSModule();
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

	public float[] getCurrentOrient() {
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

	public long getTimestamp() {
		return (currentTimestamp - timestampStart);
	}

	private void openStreamsToFiles(int id) throws FileNotFoundException {

		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/DG");

		if (!folder.exists()) {
			folder.mkdir();
		}

		File dir = new File(String.format(
				Environment.getExternalStorageDirectory() + "/DG/%d", id));
		if (!dir.exists()) {
			dir.mkdirs();
		}

		String fileName = dir.toString() + "/acc.log";
		FileOutputStream faccStream = new FileOutputStream(fileName);
		accStream = new PrintStream(faccStream);

		fileName = dir.toString() + "/gyro.log";
		FileOutputStream fgyroStream = new FileOutputStream(fileName);
		gyroStream = new PrintStream(fgyroStream);

		fileName = dir.toString() + "/mag.log";
		FileOutputStream fmagStream = new FileOutputStream(fileName);
		magStream = new PrintStream(fmagStream);

		fileName = dir.toString() + "/accwog.log";
		FileOutputStream faccwogStream = new FileOutputStream(fileName);
		accwogStream = new PrintStream(faccwogStream);

		fileName = dir.toString() + "/orient.log";
		FileOutputStream forientStream = new FileOutputStream(fileName);
		orientStream = new PrintStream(forientStream);

		if (performOrientationEstimation) {
			fileName = dir.toString() + "/myOrient.log";
			FileOutputStream fmyOrientStream = new FileOutputStream(fileName);
			myOrientStream = new PrintStream(fmyOrientStream);

			fileName = dir.toString() + "/myOrientEuler.log";
			FileOutputStream fmyOrientEulerStream = new FileOutputStream(
					fileName);
			myOrientEulerStream = new PrintStream(fmyOrientEulerStream);
		}
		
		fileName = dir.toString() + "/pressure.log";
		FileOutputStream fpressureStream = new FileOutputStream(fileName);
		pressureStream = new PrintStream(fpressureStream);

	}

	// Closing the data streams
	private void closeStreams() {
		accStream.close();
		gyroStream.close();
		magStream.close();
		accwogStream.close();
		orientStream.close();
		if (performOrientationEstimation) {
			myOrientStream.close();
			myOrientEulerStream.close();
		}
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
				
				if (save2file)
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
			
			if (save2file)
				saveToStream(pressureStream, getTimestamp(), event.values);
		}

		/**
		 * @param event
		 */
		private void processNewOrientationData(SensorEvent event) {
			if (save2file)
				saveToStream(orientStream, getTimestamp(), event.values);

			if (performOrientationEstimation) {
				float[] quaternion = new float[4];
				SensorManager.getQuaternionFromVector(quaternion,
						event.values);
				//
				orientationEstimator.correct(quaternion[0], quaternion[1],
						quaternion[2], quaternion[3]);
				// Complementary filter
				if (complementaryFilterEstimation.getState()) {
					complementaryFilterEstimation.magAccUpdate(quaternion);
				}
				
				// Getting the estimate from our AEKF
				float [] ourEstimate = orientationEstimator.getEstimate();
				
				float w = ourEstimate[0];
				float x = ourEstimate[1];
				float y = ourEstimate[2];
				float z = ourEstimate[3];

				if (save2file)
					saveToStream(myOrientStream, getTimestamp(), quaternion);

				// Should be X Y Z
				// http://kosukek.bitbucket.org/conversion-from-quaternion-to-euler-anglesxyz.html
//					float rollX = (float) (Math.atan2(2 * y * w - 2 * x * z, 1
//							- 2 * y * y - 2 * z * z) * 180.0 / Math.PI);
//					float pitchY = (float) (Math.atan2(2 * x * w - 2 * y * z, 1
//							- 2 * x * x - 2 * z * z) * 180.0 / Math.PI);
//					float yawZ = (float) (Math.asin(2 * x * y + 2 * z * w) * 180.0 / Math.PI);
				
				//http://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
				float rollX = (float) (Math.atan2(2*y*z + 2 * x * w, 1 - 2 * ( x*x + y*y) ) * 180.0 / Math.PI );
				float pitchY = (float) (Math.asin(2 * (w *y - x*z)) * 180.0 / Math.PI);
				float yawZ = (float) (Math.atan2(2*(w*z + y*x), 1-2*(y*y + z*z))* 180.0 / Math.PI);
				
				try {
					orientMtx.acquire();
					orient[0] = rollX;
					orient[1] = pitchY;
					orient[2] = yawZ;
					orientMtx.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (save2file)
					saveToStream(myOrientEulerStream, getTimestamp(),
							orient);

			}
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

			if (save2file)
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

			if (save2file)
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

			if (save2file)
				saveToStream(accStream, getTimestamp(), acc);
			
			if (stepometerStarted && stepometerRunningCounter > 200)
			{
				if ( accWindow.size() > stepometerWindowSize)
				{
					 Log.d("Main::Activity", "X");
					 
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
					 Log.d("Main::Activity", "Y");
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

}
