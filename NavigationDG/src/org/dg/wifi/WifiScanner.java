package org.dg.wifi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.channels.AlreadyConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
	long startTimestampOfWiFiScanning, startTimestampOfCurrentScan, startTimestampOfGlobalTime;
	private int id;
	
	WiFiPlaceRecognition placeRecognition = new WiFiPlaceRecognition();
	
	// Last results
	List<ScanResult> previousWiFiList = new ArrayList<ScanResult>();
	
	// Graph result
	List<wiFiMeasurement> graphWiFiList = new ArrayList<wiFiMeasurement>();
	boolean graphWiFiListReady = false;
	
	/// G2O
	// Position index
	int posIndex = 10000;
	
	// List of already found networks
	List<String> bssid2name = new ArrayList<String>();
	
	// File to save data
	PrintStream outStreamG2oTest = null;

	public WifiScanner(WifiManager _wifiManager) {
		wifiManager = _wifiManager;
		continuousScanning = false;
		singleScan = true;
		id = 0;
		startTimestampOfGlobalTime = 0;

		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/DG");

		if (!folder.exists()) {
			folder.mkdir();
		}
		
		// G2O
		String fileName = "";
		fileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/DG"
				+ "/wifi.g2o");
		id++;
		
		// Create stream to save those scans
		FileOutputStream foutStream = null;
		try {
			foutStream = new FileOutputStream(fileName);
			outStreamG2oTest = new PrintStream(foutStream);
//			outStreamG2oTest.print("FIX 0\n");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	public WifiScanner singleScan(boolean _singleScan) {
		this.singleScan = _singleScan;
		return this;
	}
	
	public WifiScanner continuousScanning(boolean _continuousScanning) {
		this.continuousScanning = _continuousScanning;
		return this;
	}
	
	public WifiScanner startTimestampOfGlobalTime(long _startTimestampOfGlobalTime)
	{
		this.startTimestampOfGlobalTime = _startTimestampOfGlobalTime/1000000;
		return this;
	}
	
	public boolean getRunningState() {
		return waitingForScan;
	}

	public void startScanning() {
		if (id == 0)
			startTimestampOfWiFiScanning = System.currentTimeMillis();
		startTimestampOfCurrentScan = System.currentTimeMillis();
		
		// We initialize the WiFi scan if WiFi is on
		if (wifiManager.isWifiEnabled() && (singleScan || continuousScanning) && !getRunningState())
		{
			wifiManager.startScan();	
			waitingForScan = true;
		}
	}

	public void stopScanning() {
		continuousScanning = false;
		if (outStreamG2oTest != null)
		{
			outStreamG2oTest.close();
		}
	}
	
	public int getNetworkCount()
	{
		return previousWiFiList.size();
	}
	
	public String getStrongestNetwork()
	{
		String bestName = "";
		int bestLvl = -150;
		
		for (int i = 0; i < previousWiFiList.size(); i++) {
			ScanResult scanResult = previousWiFiList.get(i);
			if ( scanResult.level > bestLvl)
			{
				bestLvl = scanResult.level;
				bestName = scanResult.SSID;
			}
		}
		return bestName;
	}
	
	
	public void addLastScanToRecognition()
	{
		if ( previousWiFiList.size() > 0)
			placeRecognition.addPlace(previousWiFiList);
	}
	
	public int recognizePlaceBasedOnLastScan()
	{
		return placeRecognition.recognizePlace(previousWiFiList);
	}
	
	public int getSizeOfPlaceDatabase()
	{
		return placeRecognition.getSizeOfPlaceDatabase();
	}
	
	public List<wiFiMeasurement> getGraphWiFiList() {
		if (graphWiFiListReady)
		{
			graphWiFiListReady = false;
			return new ArrayList<wiFiMeasurement>(graphWiFiList);
		}
		return null;
	}

	// Convert measurement from dBm to meters
	private double convertLevelToMeters(double level) {
		double tmp = -40 + Math.abs(level);
		tmp = tmp / 40;
		return Math.pow(10, tmp);
	}

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.d("WiFi", "Scan finished\n");
		try {
			Log.d("WiFi",
					"Timestamps: " + Long.toString(startTimestampOfCurrentScan)
							+ " " + Long.toString(startTimestampOfWiFiScanning)
							+ " " + Long.toString( startTimestampOfGlobalTime)
							+ " " + System.currentTimeMillis() + "\n");
			Log.d("WiFi",
					"Timestamps diff: "
							+ Long.toString(startTimestampOfCurrentScan
									- startTimestampOfWiFiScanning
									+ startTimestampOfGlobalTime)
							+ " "
							+ Long.toString(System.currentTimeMillis()
									- startTimestampOfCurrentScan) + "\n");

			// File to save results
			String fileName = "";
			fileName = String.format(Locale.getDefault(), Environment
					.getExternalStorageDirectory().toString()
					+ "/DG"
					+ "/%04d.wifi", id);
			id++;
			
			// Create stream to save those scans
			FileOutputStream foutStream = new FileOutputStream(fileName);
			PrintStream outStream = new PrintStream(foutStream);

			// Save the timestamp of start + length of the scan
			outStream.print(Long.toString(startTimestampOfCurrentScan - startTimestampOfWiFiScanning + startTimestampOfGlobalTime) + "\t"
					+ Long.toString(System.currentTimeMillis() - startTimestampOfCurrentScan)
					+ "\n");

			// Process list of detected WiFis
			List<ScanResult> wifiList = wifiManager.getScanResults();
			Log.d("WiFi", "Found " + wifiList.size() + " wifis \n");
			
			// Save BSSID, SSID, lvl and frequency
			for (int i = 0; i < wifiList.size(); i++) {
				ScanResult scanResult = wifiList.get(i);
				outStream.print(scanResult.BSSID + "\t" + scanResult.SSID
						+ "\t" + scanResult.level + "\t" + scanResult.frequency
						+ "\n");
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
				graphWiFiList.add(new wiFiMeasurement(index+10000, distance));
			}
			graphWiFiListReady = true;

			
			// Close cleanly
			outStream.close();
			
			// Save measurement
			previousWiFiList = wifiList;
						
		} catch (Exception e) {
			Log.d("WiFi", "Scanning failed\n");
		}

		Toast toast = Toast.makeText(arg0.getApplicationContext(),
				"WiFi scan finished", Toast.LENGTH_SHORT);
		toast.show();

		
		
		// Prepare for next measurement
		waitingForScan = false;
		if (continuousScanning) {
			startTimestampOfCurrentScan = System.currentTimeMillis();
			wifiManager.startScan();
			waitingForScan = true;
		}
	}

}
