package org.dg.wifi;

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
import android.widget.Toast;

public class WifiScanner extends BroadcastReceiver {

	WifiManager wifiManager;
	boolean scanning;
	boolean singleScan;
	long startTimestamp, startTimestampNano;
	private int id;
	//private InertialSensors inertialSensors;
	
	/*public WifiScanner(WifiManager _wifiManager, InertialSensors _inertial) {
		wifiManager = _wifiManager;
		inertialSensors = _inertial;
		scanning = false;
		singleScan = false;
		id = 0;
	}*/
	
	
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
	
	public void doSingleScan(int _id)
	{
		singleScan = true;
		id = _id;
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
				String fileName = "" ;
				if (singleScan) {
					fileName = String.format(Locale.getDefault(), Environment
							.getExternalStorageDirectory().toString()
							+ "/_exp/wifi/%04d.wifi", id);
				//	SharedData.globalInstance.write_flag = false;
				//	inertialSensors.stop();
				} else {
					fileName = String.format(Locale.getDefault(), Environment
							.getExternalStorageDirectory().toString()
							+ "/_exp/wifi/%04d.wifi", System.currentTimeMillis()
							- startTimestamp);
				}

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
			
			Toast toast = Toast.makeText(arg0.getApplicationContext(), "Single scan finished", Toast.LENGTH_SHORT);
			toast.show();
			
			
			singleScan = false;
			if ( scanning ) wifiManager.startScan();
		}
	}
}
