package org.dg.wifi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.channels.AlreadyConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

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

	WifiManager wifiManager;
	boolean continuousScanning = false, singleScan = true;
	boolean waitingForScan = false;
	long startTimestampOfWiFiScanning, startTimestampOfCurrentScan,
			startTimestampOfGlobalTime;

	// Scan id and graphId
	private int id, graphPoseId;

	WiFiPlaceRecognition placeRecognition;

	// Last results
	List<ScanResult> previousWiFiList = new ArrayList<ScanResult>();
	private final Semaphore previousScanMtx = new Semaphore(1, true);

	// Graph result
	List<wiFiMeasurement> graphWiFiList = new ArrayList<wiFiMeasurement>();
	boolean graphWiFiListReady = false;

	// / G2O
	// Position index
	int posIndex = 10000;

	// Variable indicating new measurement
	boolean newMeasurement = false;

	// List of already found networks
	List<String> bssid2name = new ArrayList<String>();

	// File to save data
	PrintStream outStreamRawData = null;
	
	// Should we save raw WiFi data
	boolean saveRawData = false;

	public WifiScanner(
			WifiManager _wifiManager,
			org.dg.openAIL.ConfigurationReader.Parameters.WiFiPlaceRecognition wifiPlaceRecognitionParameters) {
		wifiManager = _wifiManager;
		continuousScanning = false;
		singleScan = true;
		id = 0;
		graphPoseId = 0;
		startTimestampOfGlobalTime = 0;

		placeRecognition = new WiFiPlaceRecognition(
				wifiPlaceRecognitionParameters);

		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/OpenAIL/WiFi");

		if (!folder.exists()) {
			folder.mkdir();
		}

		// File to save results
		saveRawData = wifiPlaceRecognitionParameters.recordRawData;
	}

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

	public void startNewPlaceRecognitionThread() {
		placeRecognition.startRecognition();
	}

	public void stopNewPlaceRecognitionThread() {
		placeRecognition.stopRecognition();
	}

	public void setGraphPoseId(int _id) {
		graphPoseId = _id;
	}

	public boolean getRunningState() {
		return waitingForScan;
	}

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

	public void stopScanning() {
		continuousScanning = false;

//		placeRecognition.closeSaveStream();

		// Close cleanly
		if (outStreamRawData != null) {
			outStreamRawData.close();
			outStreamRawData = null;
		}
	}

	public int getNetworkCount() {
		return previousWiFiList.size();
	}

	public String getStrongestNetwork() {
		String bestName = "";
		int bestLvl = -150;

		for (int i = 0; i < previousWiFiList.size(); i++) {
			ScanResult scanResult = previousWiFiList.get(i);
			if (scanResult.level > bestLvl) {
				bestLvl = scanResult.level;
				bestName = scanResult.SSID;
			}
		}
		return bestName;
	}

	public List<MyScanResult> getLastScan()
	{
		List<MyScanResult> myList = new ArrayList<MyScanResult>();
		try {
			previousScanMtx.acquire();
			for (ScanResult sr : previousWiFiList) {
				myList.add(new MyScanResult(sr.BSSID, sr.level, sr.SSID));
			}
			previousScanMtx.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return myList;
	}
	
	public void addLastScanToRecognition(int id) {
		try {
			previousScanMtx.acquire();
			if (previousWiFiList.size() > 0) {
				List<MyScanResult> myList = new ArrayList<MyScanResult>();
				for (ScanResult sr : previousWiFiList) {
					myList.add(new MyScanResult(sr.BSSID, sr.level));
				}
				placeRecognition.addPlace(myList, id);
			}
			previousScanMtx.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void addScanToRecognition(int id, List<MyScanResult> wifiScan) {
		if (wifiScan.size() > 0)
			placeRecognition.addPlace(wifiScan, id);
	}

	public List<org.dg.openAIL.IdPair<Integer, Integer>> getAndClearRecognizedPlacesList() {
		return placeRecognition.getAndClearRecognizedPlacesList();
	}

	public int getSizeOfPlaceDatabase() {
		return placeRecognition.getSizeOfPlaceDatabase();
	}

	public List<wiFiMeasurement> getGraphWiFiList() {
		if (graphWiFiListReady) {
			graphWiFiListReady = false;
			return new ArrayList<wiFiMeasurement>(graphWiFiList);
		}
		return null;
	}

	public boolean isNewMeasurement() {
		return newMeasurement;
	}

	public void newMeasurement(boolean value) {
		newMeasurement = value;
	}

	// Convert measurement from dBm to meters
	private double convertLevelToMeters(double level) {
		double tmp = -40 + Math.abs(level);
		tmp = tmp / 40;
		return Math.pow(10, tmp);
	}
	
	public void setStartTime() {
		startTimestampOfGlobalTime = System.nanoTime();
	}

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.d("WiFi", "Scan finished\n");
		if (waitingForScan) {
			try {

				// Process list of detected WiFis
				List<ScanResult> wifiList = wifiManager.getScanResults();
				Log.d("WiFi", "Found " + wifiList.size() + " wifis \n");
				
				if (saveRawData)
				{
					outStreamRawData.print(Integer.toString(graphPoseId)
							+ "\t"
							+ Integer.toString(id)
							+ "\t"
							+ Long.toString(startTimestampOfCurrentScan
									- startTimestampOfGlobalTime)
							+ "\t"
							+ Long.toString(System.nanoTime()
									- startTimestampOfGlobalTime) + "\t");
					outStreamRawData.print(wifiList.size() + "\n");
	
					// Save BSSID, SSID, lvl and frequency
					for (int i = 0; i < wifiList.size(); i++) {
						ScanResult scanResult = wifiList.get(i); //convertLevelToMeters(scanResult.level)  + "\t" + scanResult.frequency 
						outStreamRawData.print(scanResult.BSSID + "\t"
								+ scanResult.SSID + "\t" + scanResult.level + "\n");
					}
				}
				
				// Save data for graph
				graphWiFiList.clear();
				for (int i = 0; i < wifiList.size(); i++) {
					// Getting network
					ScanResult scanResult = wifiList.get(i);
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
				previousScanMtx.acquire();
				previousWiFiList = wifiList;
				previousScanMtx.release();

				newMeasurement = true;

				Toast toast = Toast.makeText(arg0.getApplicationContext(),
						"WiFi scan finished", Toast.LENGTH_SHORT);
				toast.show();
			} catch (Exception e) {
				Log.d("WiFi", "Scanning failed: " + e.getMessage() + "\n");

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
			Log.d("WiFi",
					"Called start, waiting on next scan - startScan value: "
							+ value + "\n");
		}
		id++;

	}

}
