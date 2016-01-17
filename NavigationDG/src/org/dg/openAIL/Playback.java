//package org.dg.openAIL;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Locale;
//import java.util.Scanner;
//
//import org.dg.wifi.MyScanResult;
//
//import android.os.Environment;
//import android.util.Log;
//
//public class Playback {
//	private static final String moduleLogName = "Playback";
//	
//	public enum SOURCE_TYPE{WIFI, ACC, GYRO, MAG, ANDROID_ORIENT};
//	
//	class RawDataComparator implements Comparator<RawData> {
//		@Override
//		public int compare(RawData arg0, RawData arg1) {
//			if ( arg0 == null && arg1 == null)
//				return 0;
//			else if (arg0 == null)
//				return 1;
//			else if ( arg1 == null)
//				return -1;
//			return (int) (arg0.timestamp - arg1.timestamp);
//		}
//		
//	}
//	
//	class RawData {
//		SOURCE_TYPE sourceType;
//		long timestamp, timestampEnd;
//		double x, y, z, w;
//		int graphId, wifiId, wifiCount;
//		List<MyScanResult> wifiScans;
//		
//	}
//	
//	Scanner wifiScanner, accScanner, gyroScanner, magScanner, androidOrientScanner;
//	
//	RawData lastWifi, lastAcc, lastGyro, lastMag, lastAndroidOrient;
//	
//	Playback() {
//		File folder = new File(Environment.getExternalStorageDirectory()
//				+ "/OpenAIL/rawData");
//	
//		if (!folder.exists()) {
//			folder.mkdir();
//		}
//		
//		// Preparations to read saved, raw data
//
//		// WiFi
//		try {
//			wifiScanner = new Scanner(new BufferedReader(new FileReader(
//					folder + "/wifi.log")));
//			wifiScanner.useLocale(Locale.US);
//		} catch (FileNotFoundException e1) {
//			Log.e(moduleLogName, "Missing wifi.log");
//		}
//		
//		// Accelerometer
//		try {
//			accScanner = new Scanner(new BufferedReader(new FileReader(
//					folder + "/acc.log")));
//			accScanner.useLocale(Locale.US);
//		} catch (FileNotFoundException e1) {
//			Log.e(moduleLogName, "Missing acc.log");
//		}		
//		
//		// Gyroscope
//		try {
//			gyroScanner = new Scanner(new BufferedReader(new FileReader(
//					folder + "/gyro.log")));
//			gyroScanner.useLocale(Locale.US);
//		} catch (FileNotFoundException e1) {
//			Log.e(moduleLogName, "Missing gyro.log");
//		}
//		
//		// Magnetometer
//		try {
//			magScanner = new Scanner(new BufferedReader(new FileReader(
//					folder + "/mag.log")));
//			magScanner.useLocale(Locale.US);
//		} catch (FileNotFoundException e1) {
//			Log.e(moduleLogName, "Missing mag.log");
//		}
//		
//		// Android orient
//		try {
//			androidOrientScanner = new Scanner(new BufferedReader(new FileReader(
//					folder + "/orientAndroid.log")));
//			androidOrientScanner.useLocale(Locale.US);
//		} catch (FileNotFoundException e1) {
//			Log.e(moduleLogName, "Missing orientAndroid.log");
//		}
//		
//		init();
//	}
//	
//	void init() {
//		lastWifi = readWiFi();
//		lastAcc = readAcc();
//		lastMag = readMag();
//		lastGyro = readGyro();
//		lastAndroidOrient = readAndroidOrient();
//	}
//	
//	RawData getNextData() {
//		RawData nextData = minimum(lastWifi, lastAcc, lastMag, lastGyro, lastAndroidOrient);
//		
//		// Reading next data
//		if (nextData.sourceType == SOURCE_TYPE.WIFI)
//			lastWifi = readWiFi();
//		else if (nextData.sourceType == SOURCE_TYPE.ACC)
//			lastAcc = readAcc();
//		else if (nextData.sourceType == SOURCE_TYPE.GYRO)
//			lastGyro = readGyro();
//		else if (nextData.sourceType == SOURCE_TYPE.MAG)
//			lastMag = readMag();
//		else if (nextData.sourceType == SOURCE_TYPE.ANDROID_ORIENT)
//			lastAndroidOrient = readAndroidOrient();
//		
//		return nextData;
//	}
//	
//	RawData minimum(RawData wifi, RawData acc, RawData mag, RawData gyro, RawData andOrient) {
//		List<RawData> list = new ArrayList<RawData>();
//		list.add(wifi);
//		list.add(acc);
//		list.add(mag);
//		list.add(gyro);
//		list.add(andOrient);
////		Log.e(moduleLogName, "MINIMUM: " + wifi.timestamp);
////		Log.e(moduleLogName, "MINIMUM: " + acc.timestamp);
////		Log.e(moduleLogName, "MINIMUM: " + mag.timestamp);
////		Log.e(moduleLogName, "MINIMUM: " + gyro.timestamp);
////		Log.e(moduleLogName, "MINIMUM: " + andOrient.timestamp);
//		
//		Collections.sort(list, new RawDataComparator());
//		
//		return list.get(0);
//	}
//	
//	
//	RawData readAcc() {
//		RawData accData = readInertialSensor(accScanner, false);
//		accData.sourceType = SOURCE_TYPE.ACC;
//		return accData;
//	}
//	
//	RawData readMag() {
//		RawData magData = readInertialSensor(magScanner, false);
//		magData.sourceType = SOURCE_TYPE.MAG;
//		return magData;
//	}
//	
//	RawData readGyro() {
//		RawData gyroData = readInertialSensor(gyroScanner, false);
//		gyroData.sourceType = SOURCE_TYPE.GYRO;
//		return gyroData;
//	}
//	
//	RawData readAndroidOrient() {
//		RawData androidOrientData = readInertialSensor(androidOrientScanner, true);
//		androidOrientData.sourceType = SOURCE_TYPE.ANDROID_ORIENT;
//		return androidOrientData;
//	}
//	
//	RawData readInertialSensor(Scanner inertialScanner, boolean fourValues) {
//		if ( inertialScanner.hasNextLong() ) {
//			RawData inertialData = new RawData();
//			inertialData.timestamp = inertialScanner.nextLong();
//			inertialData.x = inertialScanner.nextDouble();
//			inertialData.y = inertialScanner.nextDouble();
//			if (fourValues)
//				inertialData.z = inertialScanner.nextDouble();
//			String endOfLine = inertialScanner.nextLine();
//			return inertialData;
//		}
//		return null;
//	}
//	
//	
//	RawData readWiFi() {
//		if ( wifiScanner.hasNextInt()) {
//			RawData wifiData = new RawData();
//			wifiData.sourceType = SOURCE_TYPE.WIFI;
//			wifiData.graphId = wifiScanner.nextInt();
//			wifiData.wifiId = wifiScanner.nextInt();
//			wifiData.timestamp = wifiScanner.nextLong();
//			wifiData.timestampEnd = wifiScanner.nextLong();
//			wifiData.wifiCount = wifiScanner.nextInt();
//			String endOfLine = wifiScanner.nextLine();
//			
//			wifiData.wifiScans = PriorMapHandler.extractListOfWiFiNetworksFromFile(wifiScanner, wifiData.wifiCount);
//	
//			Log.d(moduleLogName, "WIFI: " + wifiData.graphId + " " + wifiData.wifiId + " " + wifiData.timestamp + " " + wifiData.timestamp + " " + wifiData.wifiCount );
//			for (MyScanResult msr : wifiData.wifiScans) {
//				Log.d(moduleLogName, "\tnetwork: " + msr.BSSID + " " + msr.networkName + " " + msr.level);
//			}
//			return wifiData;
//		}
//		return null;
//	}
//	
//
//	
//
//}
