package com.example.cam;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

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
	PrintStream accStream, gyroStream, magStream, accwogStream, orientStream;

	// Starting timestamp
	long timestampStart;

	InertialSensors(SensorManager _sensorManager) {
		sensorManager = _sensorManager;
	}


	public void start() {

		timestampStart = 0;
		try {
			openStreams();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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

	}

	public void stop() {
		unregister();
		closeStreams();
	}

	public void unregister() {
		sensorManager.unregisterListener(sensorEventListener);
	}
	
	private void openStreams() throws FileNotFoundException {

		String fileName = Environment.getExternalStorageDirectory().toString() + "/_exp/inertial/acc.log";
		FileOutputStream faccStream = new FileOutputStream(fileName);
		accStream = new PrintStream(faccStream);

		fileName = Environment.getExternalStorageDirectory()
				.toString() + "/_exp/inertial/gyro.log";
		FileOutputStream fgyroStream = new FileOutputStream(fileName);
		gyroStream = new PrintStream(fgyroStream);

		fileName = Environment.getExternalStorageDirectory()
				.toString() + "/_exp/inertial/mag.log";
		FileOutputStream fmagStream = new FileOutputStream(fileName);
		magStream = new PrintStream(fmagStream);

		fileName = Environment.getExternalStorageDirectory()
				.toString() + "/_exp/inertial/accwog.log";
		FileOutputStream faccwogStream = new FileOutputStream(fileName);
		accwogStream = new PrintStream(faccwogStream);

		fileName = Environment.getExternalStorageDirectory()
				.toString() + "/_exp/inertial/orient.log";
		FileOutputStream forientStream = new FileOutputStream(fileName);
		orientStream = new PrintStream(forientStream);

	}

	private void closeStreams() {
		accStream.close();
		gyroStream.close();
		magStream.close();
		accwogStream.close();
		orientStream.close();
	}

	private SensorEventListener sensorEventListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void onSensorChanged(SensorEvent event) {

			if (timestampStart == 0)
			{
				timestampStart = event.timestamp;
			}

			
			String nl = System.getProperty("line.separator");

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				accStream.print(Long.toString(event.timestamp - timestampStart)
						+ " " + Float.toString(event.values[0]) + " "
						+ Float.toString(event.values[1]) + " "
						+ Float.toString(event.values[2]) + nl);

			} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

				gyroStream.print(Long.toString(event.timestamp - timestampStart)
						+ " " + Float.toString(event.values[0]) + " "
						+ Float.toString(event.values[1]) + " "
						+ Float.toString(event.values[2]) + nl);

			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

				magStream.print(Long.toString(event.timestamp - timestampStart)
						+ " " + Float.toString(event.values[0]) + " "
						+ Float.toString(event.values[1]) + " "
						+ Float.toString(event.values[2]) + nl);

			} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				accwogStream.print(Long.toString(event.timestamp
						- timestampStart)
						+ " "
						+ Float.toString(event.values[0])
						+ " "
						+ Float.toString(event.values[1])
						+ " "
						+ Float.toString(event.values[2]) + nl);

			} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

				orientStream.print(Long.toString(event.timestamp
						- timestampStart)
						+ " "
						+ Float.toString(event.values[0])
						+ " "
						+ Float.toString(event.values[1])
						+ " "
						+ Float.toString(event.values[2]) + nl);

			}
		}
	};

}
