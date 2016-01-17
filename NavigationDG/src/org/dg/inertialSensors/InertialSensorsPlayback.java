package org.dg.inertialSensors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.dg.openAIL.ConfigurationReader.Parameters;
import org.dg.openAIL.PriorMapHandler;
import org.dg.wifi.MyScanResult;
import org.dg.wifi.WifiScanner;

import android.hardware.Sensor;
import android.os.Environment;
import android.util.Log;

public class InertialSensorsPlayback implements Runnable {
	final String moduleLogName = "InertialDataPlayback";

	class RawData {
		int sensorType;
		long timestamp, timestampEnd;
		float[] data;
		int graphId, wifiId, wifiCount;
		List<MyScanResult> wifiScans;

	}

	class RawDataComparator implements Comparator<RawData> {
		@Override
		public int compare(RawData arg0, RawData arg1) {
			if (arg0 == null && arg1 == null)
				return 0;
			else if (arg0 == null)
				return 1;
			else if (arg1 == null)
				return -1;
			return (int) (arg0.timestamp - arg1.timestamp);
		}

	}

	Scanner rawDataScanner;
	InertialSensors inertialSensors;

	Scanner accScanner, gyroScanner, magScanner, androidOrientScanner;

	RawData lastWifi, lastAcc, lastGyro, lastMag, lastAndroidOrient;
	
	Parameters.Playback parameters;

	public InertialSensorsPlayback(Parameters.Playback _parameters, InertialSensors _inertialSensors) {
		inertialSensors = _inertialSensors;
		parameters = _parameters;
		
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/OpenAIL/rawData");

		if (!folder.exists()) {
			folder.mkdir();
		}

		// Preparations to read saved, raw data

		// Accelerometer
		try {
			accScanner = new Scanner(new BufferedReader(new FileReader(folder
					+ "/acc.log")));
			accScanner.useLocale(Locale.US);
		} catch (FileNotFoundException e1) {
			accScanner = null;
			Log.e(moduleLogName, "Missing acc.log");
		}

		// Gyroscope
		try {
			gyroScanner = new Scanner(new BufferedReader(new FileReader(folder
					+ "/gyro.log")));
			gyroScanner.useLocale(Locale.US);
		} catch (FileNotFoundException e1) {
			gyroScanner = null;
			Log.e(moduleLogName, "Missing gyro.log");
		}

		// Magnetometer
		try {
			magScanner = new Scanner(new BufferedReader(new FileReader(folder
					+ "/mag.log")));
			magScanner.useLocale(Locale.US);
		} catch (FileNotFoundException e1) {
			magScanner = null;
			Log.e(moduleLogName, "Missing mag.log");
		}

		// Android orient
		try {
			androidOrientScanner = new Scanner(new BufferedReader(
					new FileReader(folder + "/orientAndroid.log")));
			androidOrientScanner.useLocale(Locale.US);
		} catch (FileNotFoundException e1) {
			androidOrientScanner = null;
			Log.e(moduleLogName, "Missing orientAndroid.log");
		}
		
		Log.e(moduleLogName, "readAcc");
		lastAcc = readAcc();
		Log.e(moduleLogName, "readGyro");
		lastGyro = readGyro();
		Log.e(moduleLogName, "readMag");
		lastMag = readMag();
		Log.e(moduleLogName, "readAndroidOrient");
		lastAndroidOrient = readAndroidOrient();
		Log.e(moduleLogName, "getNextData");

	}

	public void start() {
		Thread simulationThread = new Thread(this, "WiFi playback thread");
		simulationThread.start();
	}

	@Override
	public void run() {
		Log.e(moduleLogName, "Starting simulation");

		//
		// TODO: Prepare inertialsensors
		//

		long startTime = System.nanoTime();

		

		RawData rawData = getNextData();

		while (rawData != null) {

			long currentTime = (long) ((System.nanoTime() - startTime)*parameters.simulationSpeed);
			
			if ( (currentTime - rawData.timestamp) / 10e6 > parameters.inertialMaxDelay )
				Log.e(moduleLogName, "We are processing data too slow! Lower simulationSpeed! " + rawData.timestamp
						+ " vs " + currentTime + " DIFF=" + (currentTime - rawData.timestamp));
			
			if (rawData.timestamp < currentTime) {

				MySensorEvent myEvent = new MySensorEvent(rawData.timestamp,
						rawData.sensorType, rawData.data);
				inertialSensors.playback(myEvent);
				rawData = getNextData();
			} else {
				try {
					Thread.sleep(parameters.inertialSleepTimeInMs, 0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		Log.e(moduleLogName, "Simulation finished");
	}

	RawData getNextData() {
		if (lastAcc == null && lastMag == null && lastGyro == null
				&& lastAndroidOrient == null)
			return null;

		RawData nextData = minimum(lastAcc, lastMag, lastGyro,
				lastAndroidOrient);

		// Reading next data
		if (nextData.sensorType == Sensor.TYPE_ACCELEROMETER)
			lastAcc = readAcc();
		else if (nextData.sensorType == Sensor.TYPE_GYROSCOPE)
			lastGyro = readGyro();
		else if (nextData.sensorType == Sensor.TYPE_MAGNETIC_FIELD)
			lastMag = readMag();
		else if (nextData.sensorType == Sensor.TYPE_ROTATION_VECTOR)
			lastAndroidOrient = readAndroidOrient();

		return nextData;
	}

	RawData minimum(RawData acc, RawData mag, RawData gyro, RawData andOrient) {
		List<RawData> list = new ArrayList<RawData>();
		list.add(acc);
		list.add(mag);
		list.add(gyro);
		list.add(andOrient);
		// Log.e(moduleLogName, "MINIMUM: " + wifi.timestamp);
		// Log.e(moduleLogName, "MINIMUM: " + acc.timestamp);
		// Log.e(moduleLogName, "MINIMUM: " + mag.timestamp);
		// Log.e(moduleLogName, "MINIMUM: " + gyro.timestamp);
		// Log.e(moduleLogName, "MINIMUM: " + andOrient.timestamp);

		Collections.sort(list, new RawDataComparator());

		return list.get(0);
	}

	RawData readAcc() {
		RawData accData = readInertialSensor(accScanner, false);
		if (accData != null)
			accData.sensorType = Sensor.TYPE_ACCELEROMETER;
		return accData;
	}

	RawData readMag() {
		RawData magData = readInertialSensor(magScanner, false);
		if (magData != null)
			magData.sensorType = Sensor.TYPE_MAGNETIC_FIELD;
		return magData;
	}

	RawData readGyro() {
		RawData gyroData = readInertialSensor(gyroScanner, false);
		if (gyroData != null)
			gyroData.sensorType = Sensor.TYPE_GYROSCOPE;
		return gyroData;
	}

	RawData readAndroidOrient() {
		RawData androidOrientData = readInertialSensor(androidOrientScanner,
				true);
		if (androidOrientData != null)
			androidOrientData.sensorType = Sensor.TYPE_ROTATION_VECTOR;
		return androidOrientData;
	}

	RawData readInertialSensor(Scanner inertialScanner, boolean fourValues) {

//		Log.e(moduleLogName, "readInertialSensor() = "
//				+ (inertialScanner == null) + " " + fourValues);
		if (inertialScanner != null && inertialScanner.hasNextLong()) {
			RawData inertialData = new RawData();
			inertialData.timestamp = inertialScanner.nextLong();

			int valuesSize = 3;
			if (fourValues)
				valuesSize = 4;

			inertialData.data = new float[valuesSize];

			inertialData.data[0] = (float) inertialScanner.nextDouble();
			inertialData.data[1] = (float) inertialScanner.nextDouble();
			inertialData.data[2] = (float) inertialScanner.nextDouble();
			if (fourValues)
				inertialData.data[3] = (float) inertialScanner.nextDouble();
			String endOfLine = inertialScanner.nextLine();

//			Log.e(moduleLogName, "READ: " + inertialData.timestamp + " "
//					+ inertialData.data[0] + " " + inertialData.data[1] + " "
//					+ inertialData.data[2]);

			return inertialData;
		}
		return null;
	}

}
