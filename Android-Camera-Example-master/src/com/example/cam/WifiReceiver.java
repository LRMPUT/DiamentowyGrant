package com.example.cam;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {

	WifiManager wifiManager;
	boolean scanning;
	boolean singleScan;
	long startTimestamp, startTimestampNano;
	
	public WifiReceiver(WifiManager _wifiManager) {
		wifiManager = _wifiManager;
		scanning = false;
		singleScan = false;
	}
	
	
	public boolean getRunningState()
	{
		return scanning;
	}
	
	public void startScanning()
	{
		scanning = true;
		startTimestamp = System.currentTimeMillis();
		wifiManager.startScan();
	}
	
	public void stopScanning()
	{
		scanning = false;
	}
	
	public void doSingleScan()
	{
		singleScan = true;
		wifiManager.startScan();
	}
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		if ( singleScan )
		{
			startTimestamp = 0;
		}
		
		if ( scanning || singleScan )
		{
			List<ScanResult> wifiList = wifiManager.getScanResults();

			try {
				String fileName = String.format(Locale.getDefault(), Environment.getExternalStorageDirectory().toString() + "/_exp/wifi/%d.wifi",
						System.currentTimeMillis() - startTimestamp);
	
				FileOutputStream foutStream = new FileOutputStream(fileName);
				PrintStream outStream = new PrintStream(foutStream);
	
				Log.d("WiFi", "Found " + wifiList.size() + " wifis \n");
				
				for (int i = 0; i < wifiList.size(); i++) {
					ScanResult scanResult = wifiList.get(i);
					outStream.print(scanResult.timestamp + "\t" + scanResult.BSSID + "\t" + scanResult.SSID
							+ "\t" + scanResult.level + "\t" + scanResult.frequency + "\n");
					
				}
	
				outStream.close();
			} catch (Exception e)
			{
				Log.d("WiFi","Scanning failed\n");
			}
			
			singleScan = false;
			if ( scanning ) wifiManager.startScan();
		}
	}
}
