package org.dg.wifi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.dg.graphManager.wiFiMeasurement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class WifiScanner extends BroadcastReceiver {
	
	final String moduleLogName = "WiFiScanner";

	// Class to access physical sensor
	WifiManager wifiManager;
	
	// Operation mode - single/continuous
	boolean continuousScanning = false, singleScan = true;
	
	// Are we waiting for the scan?
	public boolean waitingForScan = false;
	
	// Timestamps
	long startTimestampOfWiFiScanning, startTimestampOfCurrentScan,
			startTimestampOfGlobalTime;

	// Scan id and graphId
	private int id, graphPoseId;

	// Class used to process wifi scans
	WiFiPlaceRecognition placeRecognition;

	// Last results
	List<MyScanResult> previousWiFiList = new ArrayList<MyScanResult>();
	
	

	// / G2O
	// Position index
	int posIndex = 10000;

	// Variable indicating new measurement
	boolean newMeasurement = false;

	
	// File to save data
	PrintStream outStreamRawData = null;
	
	// Should we save raw WiFi data
	boolean saveRawData = false;

	//// --------
	
	// Graph result
	List<wiFiMeasurement> graphWiFiList = new ArrayList<wiFiMeasurement>();
	boolean graphWiFiListReady = false;
	
	// List of already found networks
	List<String> bssid2name = new ArrayList<String>();

	// Wifi direct measurements
		public List<wiFiMeasurement> getGraphWiFiList() {
			if (graphWiFiListReady) {
				graphWiFiListReady = false;
				return new ArrayList<wiFiMeasurement>(graphWiFiList);
			}
			return null;
		}
		
		
	
	public WifiScanner(
			WifiManager _wifiManager,
			org.dg.openAIL.ConfigurationReader.Parameters.WiFiPlaceRecognition wifiPlaceRecognitionParameters) {
		// Save physical access to sensor
		wifiManager = _wifiManager;
		
		// We operate in continuous mode
		continuousScanning = true;
		singleScan = false;
		
		// Let's assume 0's
		id = 0;
		graphPoseId = 0;
		startTimestampOfGlobalTime = 0;

		// Creating wifi place recognition
		placeRecognition = new WiFiPlaceRecognition(
				wifiPlaceRecognitionParameters);

		// Directory to story results
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/OpenAIL/WiFi");

		if (!folder.exists()) {
			folder.mkdir();
		}

		// File to save results
		saveRawData = wifiPlaceRecognitionParameters.recordRawData;
	}

	// Nicely setting parameters
	public WifiScanner singleScan(boolean _singleScan) {
		this.singleScan = _singleScan;
		return this;
	}

	public WifiScanner continuousScanning(boolean _continuousScanning) {
		this.continuousScanning = _continuousScanning;
		return this;
	}

	public WifiScanner startTimestampOfGlobalTime(
			long _startTimestampOfGlobalTime) {
		this.startTimestampOfGlobalTime = _startTimestampOfGlobalTime ;
		return this;
	}

	// We start the recognition of places
	public void startNewPlaceRecognitionThread() {
		placeRecognition.startRecognition();
	}

	// We stop the recognition of places
	public void stopNewPlaceRecognitionThread() {
		placeRecognition.stopRecognition();
	}

	// Are we waiting for the WiFi scan?
	public boolean getRunningState() {
		return waitingForScan;
	}

	// Starts scanning
	public void startScanning() {
		if (continuousScanning && saveRawData) {

			String fileName = "";
			fileName = Environment.getExternalStorageDirectory().toString()
					+ "/OpenAIL/rawData/wifi.log";

			// RawMeasurements
			FileOutputStream foutStream;
			try {
				foutStream = new FileOutputStream(fileName);
				outStreamRawData = new PrintStream(foutStream);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		
		if (id == 0)
			startTimestampOfWiFiScanning = System.currentTimeMillis();
		startTimestampOfCurrentScan = System.currentTimeMillis();

		// We initialize the WiFi scan if WiFi is on
		Log.e("WIFI",
				"WIFI START STATE: " + wifiManager.isWifiEnabled() + " "
						+ wifiManager.pingSupplicant() + " "
						+ wifiManager.getWifiState());
		if (wifiManager.isWifiEnabled() && (singleScan || continuousScanning)) // &&
																				// !getRunningState())
		{
			wifiManager.startScan();
			waitingForScan = true;
		}
	}

	// Stops scanning - we still wait for started scan
	public void stopScanning() {
		continuousScanning = false;

//		placeRecognition.closeSaveStream();

		// Close cleanly
		if (outStreamRawData != null) {
			outStreamRawData.close();
			outStreamRawData = null;
		}
	}

	
	
	// Info for GUI
	public int getNetworkCount() {
		return previousWiFiList.size();
	}

	// Info for GUI
	public String getStrongestNetwork() {
		String bestName = "";
		int bestLvl = -150;

		for (int i = 0; i < previousWiFiList.size(); i++) {
			MyScanResult scanResult = previousWiFiList.get(i);
			if (scanResult.level > bestLvl) {
				bestLvl = scanResult.level;
				bestName = scanResult.networkName;
			}
		}
		return bestName;
	}
	
	// Return the size of place database
	public int getSizeOfPlaceDatabase() {
		return placeRecognition.getSizeOfPlaceDatabase();
	}

	// Returns the last scan
	public List<MyScanResult> getLastScan()
	{
		List<MyScanResult> myList = new ArrayList<MyScanResult>();
		synchronized (previousWiFiList) {
			for (MyScanResult sr : previousWiFiList) {
				myList.add(new MyScanResult(sr.BSSID, sr.level, sr.networkName));
			}
		}
		
		return myList;
	}
	
	// Adds last scan to place recognition
	public void addLastScanToRecognition(int id) {
		graphPoseId = id; 
		synchronized (previousWiFiList) {
			addScanToRecognition(id, previousWiFiList);
		}
	}

	// Adding scan to place rec
	public void addScanToRecognition(int id, List<MyScanResult> wifiScan) {
		if (wifiScan.size() > 0)
			placeRecognition.addPlace(wifiScan, id);
	}

	// Returns the places that we recognized and clears the list
	public List<org.dg.openAIL.IdPair<Integer, Integer>> getAndClearRecognizedPlacesList() {
		return placeRecognition.getAndClearRecognizedPlacesList();
	}

	
	

	

	// Are there new Wifi scans?
	public boolean isNewMeasurement() {
		return newMeasurement;
	}

	// we set the value of new measurement
	public void newMeasurement(boolean value) {
		newMeasurement = value;
	}

	// Setting the start of the time
	public void setStartTime() {
		startTimestampOfGlobalTime = System.nanoTime();
	}

	// When data from physical sensors is received
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.d(moduleLogName, "Scan finished\n");
		if (waitingForScan) {
			try {
				// Process list of detected WiFis
				List<ScanResult> tmp = wifiManager.getScanResults();
				Log.d(moduleLogName, "Found " + tmp.size() + " wifis \n");
				
				List<MyScanResult> wifiList = new ArrayList<MyScanResult>();
				for (ScanResult network : tmp) {
					wifiList.add(new MyScanResult(network.BSSID, network.level, network.SSID));
				}
				
				processNewScan(wifiList);
				
				
				Toast toast = Toast.makeText(arg0.getApplicationContext(),
						"WiFi scan finished", Toast.LENGTH_SHORT);
				toast.show();
			} catch (Exception e) {
				Log.d(moduleLogName, "Scanning failed: " + e.getMessage() + "\n");

				Toast toast = Toast.makeText(arg0.getApplicationContext(),
						"WiFi scan FAILED", Toast.LENGTH_SHORT);
				toast.show();
			}
		}
		

		// Prepare for next measurement
		waitingForScan = false;
		if (continuousScanning) {
			startTimestampOfCurrentScan = System.nanoTime();
			boolean value = wifiManager.startScan();
			waitingForScan = true;
			Log.d(moduleLogName,
					"Called start, waiting on next scan - startScan value: "
							+ value + "\n");
		}
		id++;

	}
	
	// Simulate working with provided scan
	public void playback(List<MyScanResult> wifiScans) {
		try {
			processNewScan(wifiScans);
			
			Log.d(moduleLogName, "WiFi scan finished");

		} catch (Exception e) {
			Log.e(moduleLogName, "WiFi scan FAILED");
		}
	}

	/**
	 * Processes new scan coming either from real sensor manager or from playback
	 */
	private void processNewScan(List<MyScanResult> wifiScans) throws InterruptedException {

			// Saving raw data
			if (saveRawData)
				saveScanToRawStream(wifiScans);

			
			// Save data for graph
			graphWiFiList.clear();
			for (int i = 0; i < wifiScans.size(); i++) {
				// Getting network
				MyScanResult scanResult = wifiScans.get(i);
				// Convert MAC to index
				int index = bssid2name.indexOf(scanResult.BSSID);
				if (index == -1) {
					bssid2name.add(scanResult.BSSID);
					index = bssid2name.indexOf(scanResult.BSSID);
				}
				// Convert lvl to meters
				double distance = convertLevelToMeters(scanResult.level);
				// Add to list
				graphWiFiList.add(new wiFiMeasurement(index + 10000,
						distance));
			}
			graphWiFiListReady = true;

			// Save measurement
			synchronized (previousWiFiList) {
				previousWiFiList = wifiScans;
			}

			newMeasurement = true;

			
	}

	/**
	 * @param wifiScans
	 */
	private void saveScanToRawStream(List<MyScanResult> wifiScans) {
		outStreamRawData.print(Integer.toString(graphPoseId)
				+ "\t"
				+ Integer.toString(id)
				+ "\t"
				+ Long.toString(startTimestampOfCurrentScan
						- startTimestampOfGlobalTime)
				+ "\t"
				+ Long.toString(System.nanoTime()
						- startTimestampOfGlobalTime) + "\t");
		outStreamRawData.print(wifiScans.size() + "\n");

		// Save BSSID, SSID, lvl and frequency
		for (int i = 0; i < wifiScans.size(); i++) {
			MyScanResult scanResult = wifiScans.get(i); //convertLevelToMeters(scanResult.level)  + "\t" + scanResult.frequency 
			outStreamRawData.print(scanResult.BSSID + "\t"
					+ scanResult.networkName + "\t" + scanResult.level + "\n");
		}
	}

	/*
	 *  Convert measurement from dBm to meters with some propagation model
	 */
	private double convertLevelToMeters(double level) {
		double tmp = -40 + Math.abs(level);
		tmp = tmp / 40;
		return Math.pow(10, tmp);
	}
}
