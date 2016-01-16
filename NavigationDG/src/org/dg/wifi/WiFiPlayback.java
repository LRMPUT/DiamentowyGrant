package org.dg.wifi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.dg.openAIL.PriorMapHandler;
import org.dg.openAIL.Playback.SOURCE_TYPE;

import android.os.Environment;
import android.util.Log;

public class WiFiPlayback implements Runnable {
final String moduleLogName = "WiFiPlayback";

	class RawData {
		SOURCE_TYPE sourceType;
		long timestamp, timestampEnd;
		double x, y, z, w;
		int graphId, wifiId, wifiCount;
		List<MyScanResult> wifiScans;
		
	}
	
	Scanner rawDataScanner;
	WifiScanner wifiScanner;
	
	public WiFiPlayback(WifiScanner _wifiScanner) {
		wifiScanner = _wifiScanner;
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/OpenAIL/rawData");
	
		if (!folder.exists()) {
			folder.mkdir();
		}
		
		// Preparations to read saved, raw data

		// WiFi
		try {
			rawDataScanner = new Scanner(new BufferedReader(new FileReader(
					folder + "/wifi.log")));
			rawDataScanner.useLocale(Locale.US);
		} catch (FileNotFoundException e1) {
			Log.e(moduleLogName, "Missing wifi.log");
		}
		
	}
	
	public void start() {
		Thread simulationThread = new Thread(this, "WiFi playback thread");
		simulationThread.start();
	}
	
	@Override
	public void run() {
		Log.e(moduleLogName, "Starting simulation");
		
		wifiScanner.waitingForScan = true;
		wifiScanner.saveRawData = false;
		
		long startTime = System.nanoTime();
		RawData rawData = readWiFi();
		
		while(rawData != null) {
			
			Log.e(moduleLogName, "rawData.timestampEnd = " + rawData.timestampEnd + " vs " + (System.nanoTime() - startTime));
			if ( rawData.timestampEnd < System.nanoTime() - startTime) {
				wifiScanner.playback(rawData.wifiScans);
				rawData = readWiFi();
			}
			else
			{
				try {
					Thread.sleep(200, 0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			
		}
		Log.e(moduleLogName, "Simulation finished");
	}
	
	
	RawData readWiFi() {
		if ( rawDataScanner.hasNextInt()) {
			RawData wifiData = new RawData();
			wifiData.sourceType = SOURCE_TYPE.WIFI;
			wifiData.graphId = rawDataScanner.nextInt();
			wifiData.wifiId = rawDataScanner.nextInt();
			wifiData.timestamp = rawDataScanner.nextLong();
			wifiData.timestampEnd = rawDataScanner.nextLong();
			wifiData.wifiCount = rawDataScanner.nextInt();
			String endOfLine = rawDataScanner.nextLine();
			
			wifiData.wifiScans = PriorMapHandler.extractListOfWiFiNetworksFromFile(rawDataScanner, wifiData.wifiCount);
	
			Log.d(moduleLogName, "WIFI: " + wifiData.graphId + " " + wifiData.wifiId + " " + wifiData.timestamp + " " + wifiData.timestamp + " " + wifiData.wifiCount );
			for (MyScanResult msr : wifiData.wifiScans) {
				Log.d(moduleLogName, "\tnetwork: " + msr.BSSID + " " + msr.networkName + " " + msr.level);
			}
			return wifiData;
		}
		return null;
	}

}
