// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Michal Nowicki (michal.nowicki@put.poznan.pl)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.dg.wifi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.dg.openAIL.ConfigurationReader.Parameters;
import org.dg.openAIL.PriorMapHandler;

import android.hardware.Sensor;
import android.os.Environment;
import android.util.Log;

public class WiFiPlayback implements Runnable {
	final String moduleLogName = "WiFiPlayback";

	class RawData {
		int sourceType;
		long timestamp, timestampEnd;
		double x, y, z, w;
		int graphId, wifiId, wifiCount;
		List<MyScanResult> wifiScans;
		
	}
	
	Scanner rawDataScanner;
	WiFiScanner wifiScanner;
	
	Parameters.Playback parameters;
	
	public WiFiPlayback(Parameters.Playback _parameters, WiFiScanner _wifiScanner) {
		wifiScanner = _wifiScanner;
		parameters = _parameters;
		
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
			rawDataScanner = null;
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
		
		wifiScanner.setPlaybackState(true);
		wifiScanner.waitingForScan = true;
		wifiScanner.saveRawData = false;
		
		long startTime = System.nanoTime();
		RawData rawData = readWiFi();
		
		while(rawData != null) {
			
			long currentTime = (long) ((System.nanoTime() - startTime) * parameters.simulationSpeed);
			
			Log.e(moduleLogName, "rawData.timestampEnd = " + rawData.timestampEnd + " vs " + currentTime);
			if ( rawData.timestampEnd < currentTime) {
				wifiScanner.playback(rawData.wifiScans);
				rawData = readWiFi();
			}
			else
			{
				try {
					Thread.sleep(parameters.wifiSleepTimeInMs, 0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			
		}
		Log.e(moduleLogName, "Simulation finished");
	}
	
	
	RawData readWiFi() {
		if ( rawDataScanner != null && rawDataScanner.hasNextInt()) {
			RawData wifiData = new RawData();
			wifiData.sourceType = Sensor.TYPE_ALL;
			wifiData.graphId = rawDataScanner.nextInt();
			wifiData.wifiId = rawDataScanner.nextInt();
			wifiData.timestamp = rawDataScanner.nextLong();
			wifiData.timestampEnd = rawDataScanner.nextLong();
			wifiData.wifiCount = rawDataScanner.nextInt();
			String endOfLine = rawDataScanner.nextLine();
			
			wifiData.wifiScans = PriorMapHandler.extractListOfWiFiNetworksFromFile(rawDataScanner, wifiData.wifiCount);
	
			Log.d(moduleLogName, "WIFI: " + wifiData.graphId + " " + wifiData.wifiId + " " + wifiData.timestamp + " " + wifiData.timestamp + " " + wifiData.wifiCount );
			for (MyScanResult msr : wifiData.wifiScans) {
				Log.d(moduleLogName, "\tnetwork: " + msr.BSSID + " " + msr.networkName + " " + msr.level + " " + msr.frequency);
			}
			return wifiData;
		}
		return null;
	}

}
